package com.kingston.mmorpg.game.player.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.kingston.mmorpg.common.util.ConcurrentHashSet;
import com.kingston.mmorpg.framework.net.socket.IoSession;
import com.kingston.mmorpg.game.account.model.AccountProfile;
import com.kingston.mmorpg.game.base.SpringContext;
import com.kingston.mmorpg.game.database.user.dao.PlayerDao;
import com.kingston.mmorpg.game.database.user.entity.AccountEnt;
import com.kingston.mmorpg.game.database.user.entity.PlayerEnt;
import com.kingston.mmorpg.game.logger.LoggerUtils;
import com.kingston.mmorpg.game.login.message.ResPlayerLogin;
import com.kingston.mmorpg.game.player.model.PlayerProfile;
import com.kingston.mmorpg.game.scene.actor.Player;

@Service
public class PlayerService {

	public static final short CMD_REQ_ACCOUNT_LOGIN = 1;

	public static final short CMD_REQ_CREATE_NEW = 2;

	public static final short CMD_REQ_PLAYER_LOGIN = 3;

	public static final short CMD_REQ_SELECT_PLAYER = 4;

	public static final short CMD_RES_ACCOUNT_LOGIN = 201;

	public static final short CMD_RES_LOGIN = 202;

	/**
	 * 在线玩家列表
	 */
	private Set<Long> onlines = new ConcurrentHashSet<>();

	/** 全服所有角色的简况 */
	private ConcurrentMap<Long, PlayerProfile> playerProfiles = new ConcurrentHashMap<>();

	/** 全服所有账号的简况 */
	private ConcurrentMap<Long, AccountProfile> accountProfiles = new ConcurrentHashMap<>();

	@Autowired
	private PlayerDao playerDao;

	public void loadAllPlayerProfiles() {
		List<PlayerProfile> allPlayers = playerDao.queryAllPlayers();
		allPlayers.forEach(player -> {
			playerProfiles.put(player.getPlayerId(), player);
		});
		LoggerUtils.error("加载玩家基本数据，总量为{}", allPlayers.size());
	}

	@Cacheable(cacheNames = "player")
	public Player getPlayer(long id) {
		PlayerEnt playerEnt = playerDao.getOne(id);
		if (playerEnt != null) {
			Player player = new Player();
			player.setId(id);
			player.setPlayerEnt(playerEnt);
			return player;
		} else {
			return null;
		}
	}

	/**
	 * 保存玩家数据
	 * 
	 * @param player
	 */
	public void savePlayer(Player player) {
		SpringContext.getAysncDbService().add2Queue(player.getEntity());
	}

	public ResPlayerLogin login(IoSession session, long playerId) {
		Player player = new Player();
		session.bindDipatcher(player);
		return new ResPlayerLogin();
	}

	public Set<Long> getOnlienPlayers() {
		return new HashSet<>(this.onlines);
	}

	private void addPlayerProfile(PlayerProfile baseInfo) {
		playerProfiles.put(baseInfo.getPlayerId(), baseInfo);

		long accountId = baseInfo.getAccountId();
		// 必须将account加载并缓存
		AccountEnt account = SpringContext.getAccountService().getAccount(accountId);
		accountProfiles.putIfAbsent(accountId, new AccountProfile());
		AccountProfile accountProfile = accountProfiles.get(accountId);
		accountProfile.addPlayerProfile(baseInfo);
	}

	public AccountProfile getAccountProfiles(long accountId) {
		AccountProfile accountProfile = accountProfiles.get(accountId);
		if (accountProfile != null) {
			return accountProfile;
		}
		AccountEnt account = SpringContext.getAccountService().getAccount(accountId);
		if (account != null) {
			accountProfile = new AccountProfile();
			accountProfile.setAccountId(accountId);
			accountProfiles.putIfAbsent(accountId, accountProfile);
		}
		return accountProfile;
	}

	public void addAccountProfile(AccountEnt account) {
		long accountId = account.getId();
		if (accountProfiles.containsKey(accountId)) {
			throw new RuntimeException("账号重复-->" + accountId);
		}
		AccountProfile accountProfile = new AccountProfile();
		accountProfile.setAccountId(accountId);
		accountProfiles.put(accountId, accountProfile);
	}

	public void addExp(Player player, long exp) {

	}

}
