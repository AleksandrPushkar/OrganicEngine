/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.model.actor.instance;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.ItemsAutoDestroy;
import net.sf.l2j.gameserver.LoginServerThread;
import net.sf.l2j.gameserver.Olympiad;
import net.sf.l2j.gameserver.RecipeController;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.SevenSignsFestival;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.Universe;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.L2CharacterAI;
import net.sf.l2j.gameserver.ai.L2PlayerAI;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.cache.WarehouseCacheManager;
import net.sf.l2j.gameserver.communitybbs.BB.Forum;
import net.sf.l2j.gameserver.communitybbs.Manager.ForumsBBSManager;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.FishTable;
import net.sf.l2j.gameserver.datatables.HennaTable;
import net.sf.l2j.gameserver.datatables.HeroSkillTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.NobleSkillTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.handler.skillhandlers.SiegeFlag;
import net.sf.l2j.gameserver.handler.skillhandlers.StrSiegeAssault;
import net.sf.l2j.gameserver.handler.skillhandlers.TakeCastle;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.CoupleManager;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.instancemanager.DuelManager;
import net.sf.l2j.gameserver.instancemanager.ItemsOnGroundManager;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.FishData;
import net.sf.l2j.gameserver.model.ForceBuff;
import net.sf.l2j.gameserver.model.Inventory;
import net.sf.l2j.gameserver.model.ItemContainer;
import net.sf.l2j.gameserver.model.L2Attackable;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Fishing;
import net.sf.l2j.gameserver.model.L2HennaInstance;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Macro;
import net.sf.l2j.gameserver.model.L2ManufactureList;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Radar;
import net.sf.l2j.gameserver.model.L2RecipeList;
import net.sf.l2j.gameserver.model.L2Request;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillTargetType;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.L2SkillLearn;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.MacroList;
import net.sf.l2j.gameserver.model.PcFreight;
import net.sf.l2j.gameserver.model.PcInventory;
import net.sf.l2j.gameserver.model.PcWarehouse;
import net.sf.l2j.gameserver.model.PetInventory;
import net.sf.l2j.gameserver.model.ShortCuts;
import net.sf.l2j.gameserver.model.TradeList;
import net.sf.l2j.gameserver.model.actor.appearance.PcAppearance;
import net.sf.l2j.gameserver.model.actor.knownlist.PcKnownList;
import net.sf.l2j.gameserver.model.actor.stat.PcStat;
import net.sf.l2j.gameserver.model.actor.status.PcStatus;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.model.base.ClassLevel;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.model.base.PlayerClass;
import net.sf.l2j.gameserver.model.base.Race;
import net.sf.l2j.gameserver.model.base.SubClass;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.Duel;
import net.sf.l2j.gameserver.model.entity.L2Event;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.ChangeWaitType;
import net.sf.l2j.gameserver.serverpackets.CharInfo;
import net.sf.l2j.gameserver.serverpackets.ConfirmDlg;
import net.sf.l2j.gameserver.serverpackets.EtcStatusUpdate;
import net.sf.l2j.gameserver.serverpackets.ExAutoSoulShot;
import net.sf.l2j.gameserver.serverpackets.ExDuelUpdateUserInfo;
import net.sf.l2j.gameserver.serverpackets.ExFishingEnd;
import net.sf.l2j.gameserver.serverpackets.ExFishingStart;
import net.sf.l2j.gameserver.serverpackets.ExOlympiadMode;
import net.sf.l2j.gameserver.serverpackets.ExOlympiadUserInfo;
import net.sf.l2j.gameserver.serverpackets.ExOlympiadUserInfoSpectator;
import net.sf.l2j.gameserver.serverpackets.ExSetCompassZoneCode;
import net.sf.l2j.gameserver.serverpackets.HennaInfo;
import net.sf.l2j.gameserver.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.serverpackets.ItemList;
import net.sf.l2j.gameserver.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.serverpackets.LeaveWorld;
import net.sf.l2j.gameserver.serverpackets.MagicSkillCanceld;
import net.sf.l2j.gameserver.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.serverpackets.ObservationMode;
import net.sf.l2j.gameserver.serverpackets.ObservationReturn;
import net.sf.l2j.gameserver.serverpackets.PartySmallWindowUpdate;
import net.sf.l2j.gameserver.serverpackets.PetInventoryUpdate;
import net.sf.l2j.gameserver.serverpackets.PledgeShowInfoUpdate;
import net.sf.l2j.gameserver.serverpackets.PledgeShowMemberListDelete;
import net.sf.l2j.gameserver.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.serverpackets.PrivateStoreListBuy;
import net.sf.l2j.gameserver.serverpackets.PrivateStoreListSell;
import net.sf.l2j.gameserver.serverpackets.QuestList;
import net.sf.l2j.gameserver.serverpackets.RecipeShopSellList;
import net.sf.l2j.gameserver.serverpackets.RelationChanged;
import net.sf.l2j.gameserver.serverpackets.Ride;
import net.sf.l2j.gameserver.serverpackets.SendTradeDone;
import net.sf.l2j.gameserver.serverpackets.SetupGauge;
import net.sf.l2j.gameserver.serverpackets.ShortCutInit;
import net.sf.l2j.gameserver.serverpackets.SkillList;
import net.sf.l2j.gameserver.serverpackets.Snoop;
import net.sf.l2j.gameserver.serverpackets.SocialAction;
import net.sf.l2j.gameserver.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.serverpackets.StopMove;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.serverpackets.TargetSelected;
import net.sf.l2j.gameserver.serverpackets.TitleUpdate;
import net.sf.l2j.gameserver.serverpackets.TradeStart;
import net.sf.l2j.gameserver.serverpackets.UserInfo;
import net.sf.l2j.gameserver.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.templates.L2Armor;
import net.sf.l2j.gameserver.templates.L2ArmorType;
import net.sf.l2j.gameserver.templates.L2EtcItemType;
import net.sf.l2j.gameserver.templates.L2Henna;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.templates.L2PcTemplate;
import net.sf.l2j.gameserver.templates.L2Weapon;
import net.sf.l2j.gameserver.templates.L2WeaponType;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.gameserver.util.FloodProtector;
import net.sf.l2j.util.Point3D;
import net.sf.l2j.util.Rnd;

/**
 * This class represents all player characters in the world.<br>
 * There is always a client-thread connected to this (except if a player-store is activated upon logout).
 * @version $Revision: 1.66.2.41.2.33 $ $Date: 2005/04/11 10:06:09 $
 */
public final class L2PcInstance extends L2PlayableInstance
{
	private static final String RESTORE_SKILLS_FOR_CHAR = "SELECT skill_id,skill_level FROM character_skills WHERE char_obj_id=? AND class_index=?";
	private static final String ADD_NEW_SKILL = "INSERT INTO character_skills (char_obj_id,skill_id,skill_level,skill_name,class_index) VALUES (?,?,?,?,?)";
	private static final String UPDATE_CHARACTER_SKILL_LEVEL = "UPDATE character_skills SET skill_level=? WHERE skill_id=? AND char_obj_id=? AND class_index=?";
	private static final String DELETE_SKILL_FROM_CHAR = "DELETE FROM character_skills WHERE skill_id=? AND char_obj_id=? AND class_index=?";
	private static final String DELETE_CHAR_SKILLS = "DELETE FROM character_skills WHERE char_obj_id=? AND class_index=?";
	private static final String ADD_SKILL_SAVE = "INSERT INTO character_skills_save (char_obj_id,skill_id,skill_level,effect_count,effect_cur_time,reuse_delay,restore_type,class_index,buff_index) VALUES (?,?,?,?,?,?,?,?,?)";
	private static final String RESTORE_SKILL_SAVE = "SELECT skill_id,skill_level,effect_count,effect_cur_time, reuse_delay FROM character_skills_save WHERE char_obj_id=? AND class_index=? AND restore_type=? ORDER BY buff_index ASC";
	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_skills_save WHERE char_obj_id=? AND class_index=?";
	private static final String UPDATE_CHARACTER = "UPDATE characters SET level=?,maxHp=?,curHp=?,maxCp=?,curCp=?,maxMp=?,curMp=?,str=?,con=?,dex=?,_int=?,men=?,wit=?,face=?,hairStyle=?,hairColor=?,heading=?,x=?,y=?,z=?,exp=?,expBeforeDeath=?,sp=?,karma=?,pvpkills=?,pkkills=?,rec_have=?,rec_left=?,clanid=?,maxload=?,race=?,classid=?,deletetime=?,title=?,accesslevel=?,online=?,isin7sdungeon=?,clan_privs=?,wantspeace=?,base_class=?,onlinetime=?,in_jail=?,jail_timer=?,newbie=?,nobless=?,power_grade=?,subpledge=?,last_recom_date=?,lvl_joined_academy=?,apprentice=?,sponsor=?,varka_ketra_ally=?,clan_join_expiry_time=?,clan_create_expiry_time=?,char_name=?,death_penalty_level=? WHERE obj_id=?";
	private static final String RESTORE_CHARACTER = "SELECT account_name, obj_Id, char_name, level, maxHp, curHp, maxCp, curCp, maxMp, curMp, acc, crit, evasion, mAtk, mDef, mSpd, pAtk, pDef, pSpd, runSpd, walkSpd, str, con, dex, _int, men, wit, face, hairStyle, hairColor, sex, heading, x, y, z, movement_multiplier, attack_speed_multiplier, colRad, colHeight, exp, expBeforeDeath, sp, karma, pvpkills, pkkills, clanid, maxload, race, classid, deletetime, cancraft, title, rec_have, rec_left, accesslevel, online, char_slot, lastAccess, clan_privs, wantspeace, base_class, onlinetime, isin7sdungeon, in_jail, jail_timer, newbie, nobless, power_grade, subpledge, last_recom_date, lvl_joined_academy, apprentice, sponsor, varka_ketra_ally,clan_join_expiry_time,clan_create_expiry_time,death_penalty_level FROM characters WHERE obj_id=?";
	private static final String RESTORE_CHAR_SUBCLASSES = "SELECT class_id,exp,sp,level,class_index FROM character_subclasses WHERE char_obj_id=? ORDER BY class_index ASC";
	private static final String ADD_CHAR_SUBCLASS = "INSERT INTO character_subclasses (char_obj_id,class_id,exp,sp,level,class_index) VALUES (?,?,?,?,?,?)";
	private static final String UPDATE_CHAR_SUBCLASS = "UPDATE character_subclasses SET exp=?,sp=?,level=?,class_id=? WHERE char_obj_id=? AND class_index =?";
	private static final String DELETE_CHAR_SUBCLASS = "DELETE FROM character_subclasses WHERE char_obj_id=? AND class_index=?";
	private static final String RESTORE_CHAR_HENNAS = "SELECT slot,symbol_id FROM character_hennas WHERE char_obj_id=? AND class_index=?";
	private static final String ADD_CHAR_HENNA = "INSERT INTO character_hennas (char_obj_id,symbol_id,slot,class_index) VALUES (?,?,?,?)";
	private static final String DELETE_CHAR_HENNA = "DELETE FROM character_hennas WHERE char_obj_id=? AND slot=? AND class_index=?";
	private static final String DELETE_CHAR_HENNAS = "DELETE FROM character_hennas WHERE char_obj_id=? AND class_index=?";
	private static final String DELETE_CHAR_SHORTCUTS = "DELETE FROM character_shortcuts WHERE char_obj_id=? AND class_index=?";
	private static final String RESTORE_CHAR_RECOMS = "SELECT char_id,target_id FROM character_recommends WHERE char_id=?";
	private static final String ADD_CHAR_RECOM = "INSERT INTO character_recommends (char_id,target_id) VALUES (?,?)";
	private static final String DELETE_CHAR_RECOMS = "DELETE FROM character_recommends WHERE char_id=?";
	
	public static final int REQUEST_TIMEOUT = 15;
	public static final int STORE_PRIVATE_NONE = 0;
	public static final int STORE_PRIVATE_SELL = 1;
	public static final int STORE_PRIVATE_BUY = 3;
	public static final int STORE_PRIVATE_MANUFACTURE = 5;
	public static final int STORE_PRIVATE_PACKAGE_SELL = 8;
	
	/** The table containing all minimum level needed for each Expertise (None, D, C, B, A, S). */
	private static final int[] EXPERTISE_LEVELS =
	{
		SkillTreeTable.getInstance().getExpertiseLevel(0), // NONE
		SkillTreeTable.getInstance().getExpertiseLevel(1), // D
		SkillTreeTable.getInstance().getExpertiseLevel(2), // C
		SkillTreeTable.getInstance().getExpertiseLevel(3), // B
		SkillTreeTable.getInstance().getExpertiseLevel(4), // A
		SkillTreeTable.getInstance().getExpertiseLevel(5), // S
	};
	
	private static final int[] COMMON_CRAFT_LEVELS =
	{
		5,
		20,
		28,
		36,
		43,
		49,
		55,
		62
	};
	
	/**
	 * The Class AIAccessor.
	 */
	public class AIAccessor extends L2Character.AIAccessor
	{
		/**
		 * Instantiates a new AI accessor.
		 */
		protected AIAccessor()
		{
		}
		
		/**
		 * Gets the player.
		 * @return the player
		 */
		public L2PcInstance getPlayer()
		{
			return L2PcInstance.this;
		}
		
		/**
		 * Do pickup item.
		 * @param object the object
		 */
		public void doPickupItem(L2Object object)
		{
			L2PcInstance.this.doPickupItem(object);
		}
		
		/**
		 * Do interact.
		 * @param target the target
		 */
		public void doInteract(L2Character target)
		{
			L2PcInstance.this.doInteract(target);
		}
		
		@Override
		public void doAttack(L2Character target)
		{
			super.doAttack(target);
			
			// cancel the recent fake-death protection instantly if the player attacks or casts spells
			getPlayer().setRecentFakeDeath(false);
			for (L2CubicInstance cubic : getCubics().values())
			{
				if (cubic.getId() != L2CubicInstance.LIFE_CUBIC)
				{
					cubic.doAction(target);
				}
			}
		}
		
		@Override
		public void doCast(L2Skill skill)
		{
			super.doCast(skill);
			
			// cancel the recent fake-death protection instantly if the player attacks or casts spells
			getPlayer().setRecentFakeDeath(false);
			if (skill == null)
			{
				return;
			}
			if (!skill.isOffensive())
			{
				return;
			}
			L2Object mainTarget = skill.getFirstOfTargetList(L2PcInstance.this);
			// the code doesn't now support multiple targets
			if ((mainTarget == null) || !(mainTarget instanceof L2Character))
			{
				return;
			}
			for (L2CubicInstance cubic : getCubics().values())
			{
				if (cubic.getId() != L2CubicInstance.LIFE_CUBIC)
				{
					cubic.doAction((L2Character) mainTarget);
				}
			}
		}
	}
	
	/**
	 * Starts battle force / spell force on target.
	 * @param target the target
	 * @param skill the skill
	 */
	@Override
	public void startForceBuff(L2Character target, L2Skill skill)
	{
		if (!(target instanceof L2PcInstance))
		{
			return;
		}
		
		if (skill.getSkillType() != SkillType.FORCE_BUFF)
		{
			return;
		}
		
		if (_forceBuff == null)
		{
			_forceBuff = new ForceBuff(this, (L2PcInstance) target, skill);
		}
	}
	
	private L2GameClient _client;
	private String _accountName;
	private long _deleteTimer;
	private boolean _isOnline = false;
	private long _onlineTime;
	private long _onlineBeginTime;
	private long _lastAccess;
	private long _uptime;
	protected int _baseClass;
	protected int _activeClass;
	protected int _classIndex = 0;
	private Map<Integer, SubClass> _subClasses;
	private PcAppearance _appearance;
	private int _charId = 0x00030b7a;
	
	/** The Experience of the L2PcInstance before the last Death Penalty. */
	private long _expBeforeDeath;
	
	/** The Karma of the L2PcInstance (if higher than 0, the name of the L2PcInstance appears in red). */
	private int _karma;
	
	/** The number of player killed during a PvP (the player killed was PvP Flagged). */
	private int _pvpKills;
	
	/** The PK counter of the L2PcInstance (= Number of non PvP Flagged player killed). */
	private int _pkKills;
	
	/** The PvP Flag state of the L2PcInstance (0=White, 1=Purple). */
	private byte _pvpFlag;
	
	/** The Siege state of the L2PcInstance. */
	private byte _siegeState = 0;
	
	/** The _cur weight penalty. */
	private int _curWeightPenalty = 0;
	
	/** The _last compass zone. */
	private int _lastCompassZone; // the last compass zone update send to the client
	
	/** The _zone validate counter. */
	private byte _zoneValidateCounter = 4;
	
	/** The _is in7s dungeon. */
	private boolean _isIn7sDungeon = false;
	
	/** The _in jail. */
	private boolean _inJail = false;
	
	/** The _jail timer. */
	private long _jailTimer = 0;
	
	/** The _jail task. */
	private ScheduledFuture<?> _jailTask;
	
	/** Olympiad. */
	private boolean _inOlympiadMode = false;
	
	/** The _ olympiad start. */
	private boolean _OlympiadStart = false;
	
	/** The _olympiad game id. */
	private int _olympiadGameId = -1;
	
	/** The _olympiad side. */
	private int _olympiadSide = -1;
	
	/** Duel. */
	private boolean _isInDuel = false;
	
	/** The _duel state. */
	private int _duelState = Duel.DUELSTATE_NODUEL;
	
	/** The _duel id. */
	private int _duelId = 0;
	
	/** The _no duel reason. */
	private SystemMessageId _noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
	
	/** Boat. */
	private boolean _inBoat;
	
	/** The _boat. */
	private L2BoatInstance _boat;
	
	/** The _in boat position. */
	private Point3D _inBoatPosition;
	
	/** The _mount type. */
	private int _mountType;
	
	/** Store object used to summon the strider you are mounting *. */
	private int _mountObjectID = 0;
	
	/** The _telemode. */
	public int _telemode = 0;
	
	/** The _exploring. */
	public boolean _exploring = false;
	
	/** The _is silent moving. */
	private boolean _isSilentMoving = false;
	
	/** The _in crystallize. */
	private boolean _inCrystallize;
	
	/** The _in craft mode. */
	private boolean _inCraftMode;
	
	/** The table containing all L2RecipeList of the L2PcInstance. */
	private final Map<Integer, L2RecipeList> _dwarvenRecipeBook = new FastMap<>();
	
	/** The _common recipe book. */
	private final Map<Integer, L2RecipeList> _commonRecipeBook = new FastMap<>();
	
	/** True if the L2PcInstance is sitting. */
	private boolean _waitTypeSitting;
	
	/** True if the L2PcInstance is using the relax skill. */
	private boolean _relax;
	
	/** Location before entering Observer Mode. */
	private int _obsX;
	
	/** The _obs y. */
	private int _obsY;
	
	/** The _obs z. */
	private int _obsZ;
	
	/** The _observer mode. */
	private boolean _observerMode = false;
	
	/** Stored from last ValidatePosition *. */
	private final Point3D _lastClientPosition = new Point3D(0, 0, 0);
	
	/** The _last server position. */
	private final Point3D _lastServerPosition = new Point3D(0, 0, 0);
	
	/** The number of recommendation obtained by the L2PcInstance. */
	private int _recomHave; // how much I was recommended by others
	
	/** The number of recommendation that the L2PcInstance can give. */
	private int _recomLeft; // how many recommendations I can give to others
	
	/** Date when recommendation points were updated last time. */
	private long _lastRecomUpdate;
	
	/** List with the recommendations that I've give. */
	private final List<Integer> _recomChars = new FastList<>();
	
	private final PcInventory _inventory = new PcInventory(this);
	
	/** The _warehouse. */
	private PcWarehouse _warehouse;
	
	/** The _freight. */
	private final PcFreight _freight = new PcFreight(this);
	
	/** The Private Store type of the L2PcInstance (STORE_PRIVATE_NONE=0, STORE_PRIVATE_SELL=1, sellmanage=2, STORE_PRIVATE_BUY=3, buymanage=4, STORE_PRIVATE_MANUFACTURE=5). */
	private int _privatestore;
	
	/** The _active trade list. */
	private TradeList _activeTradeList;
	
	/** The _active warehouse. */
	private ItemContainer _activeWarehouse;
	
	/** The _create list. */
	private L2ManufactureList _createList;
	
	/** The _sell list. */
	private TradeList _sellList;
	
	/** The _buy list. */
	private TradeList _buyList;
	
	/** True if the L2PcInstance is newbie. */
	private boolean _newbie;
	
	/** The _noble. */
	private boolean _noble = false;
	
	/** The _hero. */
	private boolean _hero = false;
	
	/** The L2FolkInstance corresponding to the last Folk which one the player talked. */
	private L2FolkInstance _lastFolkNpc = null;
	
	/** Last NPC Id talked on a quest. */
	private int _questNpcObject = 0;
	
	/** The table containing all Quests began by the L2PcInstance. */
	private final Map<String, QuestState> _quests = new FastMap<>();
	
	/** The list containing all shortCuts of this L2PcInstance. */
	private final ShortCuts _shortCuts = new ShortCuts(this);
	
	/** The list containing all macroses of this L2PcInstance. */
	private final MacroList _macroses = new MacroList(this);
	
	/** The _snoop listener. */
	private final List<L2PcInstance> _snoopListener = new FastList<>();
	
	/** The _snooped player. */
	private final List<L2PcInstance> _snoopedPlayer = new FastList<>();
	
	/** The _skill learning class id. */
	private ClassId _skillLearningClassId;
	
	// hennas
	/** The _henna. */
	private final L2HennaInstance[] _henna = new L2HennaInstance[3];
	
	/** The _henna str. */
	private int _hennaSTR;
	
	/** The _henna int. */
	private int _hennaINT;
	
	/** The _henna dex. */
	private int _hennaDEX;
	
	/** The _henna men. */
	private int _hennaMEN;
	
	/** The _henna wit. */
	private int _hennaWIT;
	
	/** The _henna con. */
	private int _hennaCON;
	
	/** The L2Summon of the L2PcInstance. */
	private L2Summon _summon = null;
	// apparently, a L2PcInstance CAN have both a summon AND a tamed beast at the same time!!
	/** The _tamed beast. */
	private L2TamedBeastInstance _tamedBeast = null;
	
	// client radar
	// TODO: This needs to be better integrated and saved/loaded
	/** The _radar. */
	private L2Radar _radar;
	
	// these values are only stored temporarily
	/** The _party matching automatic registration. */
	private boolean _partyMatchingAutomaticRegistration;
	
	/** The _party matching show level. */
	private boolean _partyMatchingShowLevel;
	
	/** The _party matching show class. */
	private boolean _partyMatchingShowClass;
	
	/** The _party matching memo. */
	private String _partyMatchingMemo;
	
	// Clan related attributes
	/** The Clan Identifier of the L2PcInstance. */
	private int _clanId;
	
	/** The Clan object of the L2PcInstance. */
	private L2Clan _clan;
	
	/** Apprentice and Sponsor IDs. */
	private int _apprentice = 0;
	
	/** The _sponsor. */
	private int _sponsor = 0;
	
	/** The _clan join expire time. */
	private long _clanJoinExpiryTime;
	
	/** The _clan create expire time. */
	private long _clanCreateExpiryTime;
	
	/** The _power grade. */
	private int _powerGrade = 0;
	
	/** The _clan privileges. */
	private int _clanPrivileges = 0;
	
	/** L2PcInstance's pledge class (knight, Baron, etc.) */
	private int _pledgeClass = 0;
	
	/** The _pledge type. */
	private int _pledgeType = 0;
	
	/** Level at which the player joined the clan as an academy member. */
	private int _lvlJoinedAcademy = 0;
	
	/** The _wants peace. */
	private int _wantsPeace = 0;
	
	// Death Penalty Buff Level
	/** The _death penalty buff level. */
	private int _deathPenaltyBuffLevel = 0;
	
	// GM related variables
	/** The _isGm. */
	private boolean _isGm;
	
	/** The _access level. */
	private int _accessLevel;
	
	/** The _chat banned. */
	private boolean _chatBanned = false; // Chat Banned
	
	/** The _chat unban task. */
	private ScheduledFuture<?> _chatUnbanTask = null;
	
	/** The _message refusal. */
	private boolean _messageRefusal = false; // message refusal mode
	
	/** The _diet mode. */
	private boolean _dietMode = false; // ignore weight penalty
	
	/** The _trade refusal. */
	private boolean _tradeRefusal = false; // Trade refusal
	
	/** The _exchange refusal. */
	private boolean _exchangeRefusal = false; // Exchange refusal
	
	/** The _party. */
	private L2Party _party;
	
	// this is needed to find the inviting player for Party response
	// there can only be one active party request at once
	/** The _active requester. */
	private L2PcInstance _activeRequester;
	
	/** The _request expire time. */
	private long _requestExpireTime = 0;
	
	/** The _request. */
	private final L2Request _request = new L2Request(this);
	
	/** The _arrow item. */
	private L2ItemInstance _arrowItem;
	
	// Used for protection after teleport
	/** The _protect end time. */
	private long _protectEndTime = 0;
	
	// protects a char from agro mobs when getting up from fake death
	/** The _recent fake death end time. */
	private long _recentFakeDeathEndTime = 0;
	
	/** The fists L2Weapon of the L2PcInstance (used when no weapon is equiped). */
	private L2Weapon _fistsWeaponItem;
	
	/** The _chars. */
	private final Map<Integer, String> _chars = new FastMap<>();
	
	// private byte _updateKnownCounter = 0;
	
	/** The current higher Expertise of the L2PcInstance (None=0, D=1, C=2, B=3, A=4, S=5). */
	private int _expertiseIndex; // index in EXPERTISE_LEVELS
	
	/** The _expertise penalty. */
	private int _expertisePenalty = 0;
	
	/** The _active enchant item. */
	private L2ItemInstance _activeEnchantItem = null;
	
	/** The _inventory disable. */
	protected boolean _inventoryDisable = false;
	
	/** The _cubics. */
	protected Map<Integer, L2CubicInstance> _cubics = new FastMap<>();
	
	/** Active shots. A FastSet variable would actually suffice but this was changed to fix threading stability... */
	protected Map<Integer, Integer> _activeSoulShots = new FastMap<Integer, Integer>().shared();
	
	/** The soul shot lock. */
	public final ReentrantLock soulShotLock = new ReentrantLock();
	
	/** Event parameters. */
	public int eventX;
	
	/** The event y. */
	public int eventY;
	
	/** The event z. */
	public int eventZ;
	
	/** The eventkarma. */
	public int eventkarma;
	
	/** The eventpvpkills. */
	public int eventpvpkills;
	
	/** The eventpkkills. */
	public int eventpkkills;
	
	/** The event title. */
	public String eventTitle;
	
	/** The kills. */
	public LinkedList<String> kills = new LinkedList<>();
	
	/** The event sit forced. */
	public boolean eventSitForced = false;
	
	/** The at event. */
	public boolean atEvent = false;
	
	/** new loto ticket *. */
	private final int _loto[] = new int[5];
	// public static int _loto_nums[] = {0,1,2,3,4,5,6,7,8,9,};
	/** new race ticket *. */
	private final int _race[] = new int[2];
	
	/** The _block list. */
	private final BlockList _blockList = new BlockList();
	
	/** The _team. */
	private int _team = 0;
	
	/** lvl of alliance with ketra orcs or varka silenos, used in quests and aggro checks [-5,-1] varka, 0 neutral, [1,5] ketra. */
	private int _alliedVarkaKetra = 0;
	
	/** The _fish combat. */
	private L2Fishing _fishCombat;
	
	/** The _fishing. */
	private boolean _fishing = false;
	
	/** The _fishx. */
	private int _fishx = 0;
	
	/** The _fishy. */
	private int _fishy = 0;
	
	/** The _fishz. */
	private int _fishz = 0;
	
	/** The _task rent pet. */
	private ScheduledFuture<?> _taskRentPet;
	
	/** The _task water. */
	private ScheduledFuture<?> _taskWater;
	
	/** Bypass validations. */
	private final List<String> _validBypass = new FastList<>();
	
	/** The _valid bypass2. */
	private final List<String> _validBypass2 = new FastList<>();
	
	/** The _forum mail. */
	private Forum _forumMail;
	
	/** The _forum memo. */
	private Forum _forumMemo;
	
	/** Current skill in use. */
	private SkillDat _currentSkill;
	
	/** Skills queued because a skill is already in progress. */
	private SkillDat _queuedSkill;
	
	/** Flag to disable equipment/skills while wearing formal wear */
	private boolean _IsWearingFormalWear = false;
	
	/** The _cursed weapon equipped id. */
	private int _cursedWeaponEquipedId = 0;
	
	/** The _revive requested. */
	private int _reviveRequested = 0;
	
	/** The _revive power. */
	private double _revivePower = 0;
	
	/** The _revive pet. */
	private boolean _revivePet = false;
	
	/** The _cp update inc check. */
	private double _cpUpdateIncCheck = .0;
	
	/** The _cp update dec check. */
	private double _cpUpdateDecCheck = .0;
	
	/** The _cp update interval. */
	private double _cpUpdateInterval = .0;
	
	/** The _mp update inc check. */
	private double _mpUpdateIncCheck = .0;
	
	/** The _mp update dec check. */
	private double _mpUpdateDecCheck = .0;
	
	/** The _mp update interval. */
	private double _mpUpdateInterval = .0;
	
	/** Herbs Task Time *. */
	private int _herbstask = 0;
	
	/**
	 * Task for Herbs.
	 */
	public class HerbTask implements Runnable
	{
		
		/** The _process. */
		private final String _process;
		
		/** The _item id. */
		private final int _itemId;
		
		/** The _count. */
		private final int _count;
		
		/** The _reference. */
		private final L2Object _reference;
		
		/** The _send message. */
		private final boolean _sendMessage;
		
		/**
		 * Instantiates a new herb task.
		 * @param process the process
		 * @param itemId the item id
		 * @param count the count
		 * @param reference the reference
		 * @param sendMessage the send message
		 */
		HerbTask(String process, int itemId, int count, L2Object reference, boolean sendMessage)
		{
			_process = process;
			_itemId = itemId;
			_count = count;
			_reference = reference;
			_sendMessage = sendMessage;
		}
		
		@Override
		@SuppressWarnings("synthetic-access")
		public void run()
		{
			try
			{
				addItem(_process, _itemId, _count, _reference, _sendMessage);
			}
			catch (Throwable t)
			{
				_log.log(Level.WARNING, "", t);
			}
		}
	}
	
	// L2JMOD Wedding
	/** The _married. */
	private boolean _married = false;
	
	/** The _partner id. */
	private int _partnerId = 0;
	
	/** The _couple id. */
	private int _coupleId = 0;
	
	/** The _engagerequest. */
	private boolean _engagerequest = false;
	
	/** The _engageid. */
	private int _engageid = 0;
	
	/** The _marryrequest. */
	private boolean _marryrequest = false;
	
	/** The _marryaccepted. */
	private boolean _marryaccepted = false;
	
	// Current force buff this caster is casting to a target
	/** The _force buff. */
	protected ForceBuff _forceBuff;
	
	/**
	 * Skill casting information (used to queue when several skills are cast in a short time) *.
	 */
	public class SkillDat
	{
		
		/** The _skill. */
		private final L2Skill _skill;
		
		/** The _ctrl pressed. */
		private final boolean _ctrlPressed;
		
		/** The _shift pressed. */
		private final boolean _shiftPressed;
		
		/**
		 * Instantiates a new skill dat.
		 * @param skill the skill
		 * @param ctrlPressed the ctrl pressed
		 * @param shiftPressed the shift pressed
		 */
		protected SkillDat(L2Skill skill, boolean ctrlPressed, boolean shiftPressed)
		{
			_skill = skill;
			_ctrlPressed = ctrlPressed;
			_shiftPressed = shiftPressed;
		}
		
		/**
		 * Checks if is ctrl pressed.
		 * @return true, if is ctrl pressed
		 */
		public boolean isCtrlPressed()
		{
			return _ctrlPressed;
		}
		
		/**
		 * Checks if is shift pressed.
		 * @return true, if is shift pressed
		 */
		public boolean isShiftPressed()
		{
			return _shiftPressed;
		}
		
		/**
		 * Gets the skill.
		 * @return the skill
		 */
		public L2Skill getSkill()
		{
			return _skill;
		}
		
		/**
		 * Gets the skill id.
		 * @return the skill id
		 */
		public int getSkillId()
		{
			return (getSkill() != null) ? getSkill().getId() : -1;
		}
	}
	
	/**
	 * Create a new L2PcInstance and add it in the characters table of the database.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Create a new L2PcInstance with an account name</li> <li>Set the name, the Hair Style, the Hair Color and the Face type of the L2PcInstance</li> <li>Add the player in the characters table of the database</li><BR>
	 * <BR>
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2PcTemplate to apply to the L2PcInstance
	 * @param accountName The name of the L2PcInstance
	 * @param name The name of the L2PcInstance
	 * @param hairStyle The hair style Identifier of the L2PcInstance
	 * @param hairColor The hair color Identifier of the L2PcInstance
	 * @param face The face type Identifier of the L2PcInstance
	 * @param sex the sex
	 * @return The L2PcInstance added to the database or null
	 */
	public static L2PcInstance create(int objectId, L2PcTemplate template, String accountName, String name, byte hairStyle, byte hairColor, byte face, boolean sex)
	{
		// Create a new L2PcInstance with an account name
		PcAppearance app = new PcAppearance(face, hairColor, hairStyle, sex);
		L2PcInstance player = new L2PcInstance(objectId, template, accountName, app);
		
		// Set the name of the L2PcInstance
		player.setName(name);
		
		// Set the base class ID to that of the actual class ID.
		player.setBaseClass(player.getClassId());
		
		if (Config.ALT_GAME_NEW_CHAR_ALWAYS_IS_NEWBIE)
		{
			player.setNewbie(true);
		}
		
		// Add the player in the characters table of the database
		boolean ok = player.createDb();
		
		if (!ok)
		{
			return null;
		}
		
		return player;
	}
	
	/**
	 * Creates the dummy player.
	 * @param objectId the object id
	 * @param name the name
	 * @return the l2 pc instance
	 */
	public static L2PcInstance createDummyPlayer(int objectId, String name)
	{
		// Create a new L2PcInstance with an account name
		L2PcInstance player = new L2PcInstance(objectId);
		player.setName(name);
		
		return player;
	}
	
	/**
	 * Gets the account name.
	 * @return the account name
	 */
	public String getAccountName()
	{
		return getClient().getAccountName();
	}
	
	/**
	 * Gets the account chars.
	 * @return the account chars
	 */
	public Map<Integer, String> getAccountChars()
	{
		return _chars;
	}
	
	/**
	 * Gets the relation.
	 * @param target the target
	 * @return the relation
	 */
	public int getRelation(L2PcInstance target)
	{
		int result = 0;
		
		// karma and pvp may not be required
		if (getPvpFlag() != 0)
		{
			result |= RelationChanged.RELATION_PVP_FLAG;
		}
		if (getKarma() > 0)
		{
			result |= RelationChanged.RELATION_HAS_KARMA;
		}
		
		if (isClanLeader())
		{
			result |= RelationChanged.RELATION_LEADER;
		}
		
		if (getSiegeState() != 0)
		{
			result |= RelationChanged.RELATION_INSIEGE;
			if (getSiegeState() != target.getSiegeState())
			{
				result |= RelationChanged.RELATION_ENEMY;
			}
			else
			{
				result |= RelationChanged.RELATION_ALLY;
			}
			if (getSiegeState() == 1)
			{
				result |= RelationChanged.RELATION_ATTACKER;
			}
		}
		
		if ((getClan() != null) && (target.getClan() != null))
		{
			if ((target.getPledgeType() != L2Clan.SUBUNIT_ACADEMY) && target.getClan().isAtWarWith(getClan().getClanId()))
			{
				result |= RelationChanged.RELATION_1SIDED_WAR;
				if (getClan().isAtWarWith(target.getClan().getClanId()))
				{
					result |= RelationChanged.RELATION_MUTUAL_WAR;
				}
			}
		}
		return result;
	}
	
	/**
	 * Retrieve a L2PcInstance from the characters table of the database and add it in _allObjects of the L2world (call restore method).<BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Retrieve the L2PcInstance from the characters table of the database</li> <li>Add the L2PcInstance object in _allObjects</li> <li>Set the x,y,z position of the L2PcInstance and make it invisible</li> <li>Update the overloaded status of the L2PcInstance</li><BR>
	 * @param objectId Identifier of the object to initialized
	 * @return The L2PcInstance loaded from the database
	 */
	public static L2PcInstance load(int objectId)
	{
		return restore(objectId);
	}
	
	/**
	 * Inits the pc status update values.
	 */
	private void initPcStatusUpdateValues()
	{
		_cpUpdateInterval = getMaxCp() / 352.0;
		_cpUpdateIncCheck = getMaxCp();
		_cpUpdateDecCheck = getMaxCp() - _cpUpdateInterval;
		_mpUpdateInterval = getMaxMp() / 352.0;
		_mpUpdateIncCheck = getMaxMp();
		_mpUpdateDecCheck = getMaxMp() - _mpUpdateInterval;
	}
	
	/**
	 * Constructor of L2PcInstance (use L2Character constructor).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Call the L2Character constructor to create an empty _skills slot and copy basic Calculator set to this L2PcInstance</li> <li>Set the name of the L2PcInstance</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method SET the level of the L2PcInstance to 1</B></FONT><BR>
	 * <BR>
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2PcTemplate to apply to the L2PcInstance
	 * @param accountName The name of the account including this L2PcInstance
	 * @param app the app
	 */
	private L2PcInstance(int objectId, L2PcTemplate template, String accountName, PcAppearance app)
	{
		super(objectId, template);
		getKnownList(); // init knownlist
		getStat(); // init stats
		getStatus(); // init status
		super.initCharStatusUpdateValues();
		initPcStatusUpdateValues();
		
		_accountName = accountName;
		_appearance = app;
		
		// Create an AI
		_ai = new L2PlayerAI(new L2PcInstance.AIAccessor());
		
		// Create a L2Radar object
		_radar = new L2Radar(this);
		
		// Retrieve from the database all skills of this L2PcInstance and add them to _skills
		// Retrieve from the database all items of this L2PcInstance and add them to _inventory
		getInventory().restore();
		if (!Config.WAREHOUSE_CACHE)
		{
			getWarehouse();
		}
		getFreight().restore();
	}
	
	/**
	 * Instantiates a new l2 pc instance.
	 * @param objectId the object id
	 */
	private L2PcInstance(int objectId)
	{
		super(objectId, null);
		getKnownList(); // init knownlist
		getStat(); // init stats
		getStatus(); // init status
		super.initCharStatusUpdateValues();
		initPcStatusUpdateValues();
	}
	
	@Override
	public final PcKnownList getKnownList()
	{
		if ((super.getKnownList() == null) || !(super.getKnownList() instanceof PcKnownList))
		{
			setKnownList(new PcKnownList(this));
		}
		return (PcKnownList) super.getKnownList();
	}
	
	@Override
	public final PcStat getStat()
	{
		if ((super.getStat() == null) || !(super.getStat() instanceof PcStat))
		{
			setStat(new PcStat(this));
		}
		return (PcStat) super.getStat();
	}
	
	@Override
	public final PcStatus getStatus()
	{
		if ((super.getStatus() == null) || !(super.getStatus() instanceof PcStatus))
		{
			setStatus(new PcStatus(this));
		}
		return (PcStatus) super.getStatus();
	}
	
	/**
	 * Gets the appearance.
	 * @return the appearance
	 */
	public final PcAppearance getAppearance()
	{
		return _appearance;
	}
	
	/**
	 * Return the base L2PcTemplate link to the L2PcInstance.
	 * @return the base template
	 */
	public final L2PcTemplate getBaseTemplate()
	{
		return CharTemplateTable.getInstance().getTemplate(_baseClass);
	}
	
	/**
	 * Return the L2PcTemplate link to the L2PcInstance.
	 * @return the template
	 */
	@Override
	public final L2PcTemplate getTemplate()
	{
		return (L2PcTemplate) super.getTemplate();
	}
	
	/**
	 * Sets the template.
	 * @param newclass the new template
	 */
	public void setTemplate(ClassId newclass)
	{
		super.setTemplate(CharTemplateTable.getInstance().getTemplate(newclass));
	}
	
	/**
	 * Return the AI of the L2PcInstance (create it if necessary).
	 * @return the aI
	 */
	@Override
	public L2CharacterAI getAI()
	{
		if (_ai == null)
		{
			synchronized (this)
			{
				if (_ai == null)
				{
					_ai = new L2PlayerAI(new L2PcInstance.AIAccessor());
				}
			}
		}
		
		return _ai;
	}
	
	/**
	 * Calculate a destination to explore the area and set the AI Intension to AI_INTENTION_MOVE_TO.
	 */
	public void explore()
	{
		if (!_exploring)
		{
			return;
		}
		
		if (getMountType() == 2)
		{
			return;
		}
		
		// Calculate the destination point (random)
		int x = (getX() + Rnd.nextInt(6000)) - 3000;
		int y = (getY() + Rnd.nextInt(6000)) - 3000;
		
		if (x > Universe.MAX_X)
		{
			x = Universe.MAX_X;
		}
		if (x < Universe.MIN_X)
		{
			x = Universe.MIN_X;
		}
		if (y > Universe.MAX_Y)
		{
			y = Universe.MAX_Y;
		}
		if (y < Universe.MIN_Y)
		{
			y = Universe.MIN_Y;
		}
		
		int z = getZ();
		
		L2CharPosition pos = new L2CharPosition(x, y, z, 0);
		
		// Set the AI Intention to AI_INTENTION_MOVE_TO
		getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, pos);
	}
	
	/**
	 * Return the Level of the L2PcInstance.
	 * @return the level
	 */
	@Override
	public final int getLevel()
	{
		return getStat().getLevel();
	}
	
	/**
	 * Return the _newbie state of the L2PcInstance.
	 * @return true, if is newbie
	 */
	public boolean isNewbie()
	{
		return _newbie;
	}
	
	/**
	 * Set the _newbie state of the L2PcInstance.
	 * @param isNewbie The Identifier of the _newbie state
	 */
	public void setNewbie(boolean isNewbie)
	{
		_newbie = isNewbie;
	}
	
	/**
	 * Sets the base class.
	 * @param baseClass the new base class
	 */
	public void setBaseClass(int baseClass)
	{
		_baseClass = baseClass;
	}
	
	/**
	 * Sets the base class.
	 * @param classId the new base class
	 */
	public void setBaseClass(ClassId classId)
	{
		_baseClass = classId.ordinal();
	}
	
	/**
	 * Checks if is in store mode.
	 * @return true, if is in store mode
	 */
	public boolean isInStoreMode()
	{
		return (getPrivateStoreType() > 0);
	}
	
	/**
	 * Checks if is in craft mode.
	 * @return true, if is in craft mode
	 */
	public boolean isInCraftMode()
	{
		return _inCraftMode;
	}
	
	/**
	 * Checks if is in craft mode.
	 * @param b the b
	 */
	public void isInCraftMode(boolean b)
	{
		_inCraftMode = b;
	}
	
	/**
	 * Manage Logout Task.
	 */
	public void logout()
	{
		closeNetConnection();
	}
	
	/**
	 * Return a table containing all Common L2RecipeList of the L2PcInstance.
	 * @return the common recipe book
	 */
	public L2RecipeList[] getCommonRecipeBook()
	{
		return _commonRecipeBook.values().toArray(new L2RecipeList[_commonRecipeBook.values().size()]);
	}
	
	/**
	 * Return a table containing all Dwarf L2RecipeList of the L2PcInstance.
	 * @return the dwarven recipe book
	 */
	public L2RecipeList[] getDwarvenRecipeBook()
	{
		return _dwarvenRecipeBook.values().toArray(new L2RecipeList[_dwarvenRecipeBook.values().size()]);
	}
	
	/**
	 * Add a new L2RecipList to the table _commonrecipebook containing all L2RecipeList of the L2PcInstance.
	 * @param recipe The L2RecipeList to add to the _recipebook
	 */
	public void registerCommonRecipeList(L2RecipeList recipe)
	{
		_commonRecipeBook.put(recipe.getId(), recipe);
	}
	
	/**
	 * Add a new L2RecipList to the table _recipebook containing all L2RecipeList of the L2PcInstance.
	 * @param recipe The L2RecipeList to add to the _recipebook
	 */
	public void registerDwarvenRecipeList(L2RecipeList recipe)
	{
		_dwarvenRecipeBook.put(recipe.getId(), recipe);
	}
	
	/**
	 * Checks for recipe list.
	 * @param recipeId the recipe id
	 * @return true, if successful <b>TRUE</b> if player has the recipe on Common or Dwarven Recipe book else returns <b>FALSE</b>
	 */
	public boolean hasRecipeList(int recipeId)
	{
		if (_dwarvenRecipeBook.containsKey(recipeId))
		{
			return true;
		}
		else if (_commonRecipeBook.containsKey(recipeId))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Tries to remove a L2RecipList from the table _DwarvenRecipeBook or from table _CommonRecipeBook, those table contain all L2RecipeList of the L2PcInstance.
	 * @param recipeId the recipe id
	 */
	public void unregisterRecipeList(int recipeId)
	{
		if (_dwarvenRecipeBook.containsKey(recipeId))
		{
			_dwarvenRecipeBook.remove(recipeId);
		}
		else if (_commonRecipeBook.containsKey(recipeId))
		{
			_commonRecipeBook.remove(recipeId);
		}
		else
		{
			_log.warning("Attempted to remove unknown RecipeList: " + recipeId);
		}
		
		L2ShortCut[] allShortCuts = getAllShortCuts();
		for (L2ShortCut sc : allShortCuts)
		{
			if ((sc != null) && (sc.getId() == recipeId) && (sc.getType() == L2ShortCut.TYPE_RECIPE))
			{
				deleteShortCut(sc.getSlot(), sc.getPage());
			}
		}
	}
	
	/**
	 * Returns the Id for the last talked quest NPC.
	 * @return the last quest npc object
	 */
	public int getLastQuestNpcObject()
	{
		return _questNpcObject;
	}
	
	/**
	 * Sets the last quest npc object.
	 * @param npcId the new last quest npc object
	 */
	public void setLastQuestNpcObject(int npcId)
	{
		_questNpcObject = npcId;
	}
	
	/**
	 * Return the QuestState object corresponding to the quest name.
	 * @param quest The name of the quest
	 * @return the quest state
	 */
	public QuestState getQuestState(String quest)
	{
		return _quests.get(quest);
	}
	
	/**
	 * Add a QuestState to the table _quest containing all quests began by the L2PcInstance.
	 * @param qs The QuestState to add to _quest
	 */
	public void setQuestState(QuestState qs)
	{
		_quests.put(qs.getQuestName(), qs);
	}
	
	/**
	 * Remove a QuestState from the table _quest containing all quests began by the L2PcInstance.
	 * @param quest The name of the quest
	 */
	public void delQuestState(String quest)
	{
		_quests.remove(quest);
	}
	
	/**
	 * Adds the to quest state array.
	 * @param questStateArray the quest state array
	 * @param state the state
	 * @return the quest state[]
	 */
	private QuestState[] addToQuestStateArray(QuestState[] questStateArray, QuestState state)
	{
		int len = questStateArray.length;
		QuestState[] tmp = new QuestState[len + 1];
		for (int i = 0; i < len; i++)
		{
			tmp[i] = questStateArray[i];
		}
		tmp[len] = state;
		return tmp;
	}
	
	/**
	 * Return a table containing all Quest in progress from the table _quests.
	 * @return the all active quests
	 */
	public Quest[] getAllActiveQuests()
	{
		FastList<Quest> quests = new FastList<>();
		
		for (QuestState qs : _quests.values())
		{
			if (qs.getQuest().getQuestIntId() >= 1999)
			{
				continue;
			}
			
			if (qs.isCompleted() && !Config.DEVELOPER)
			{
				continue;
			}
			
			if (!qs.isStarted() && !Config.DEVELOPER)
			{
				continue;
			}
			
			quests.add(qs.getQuest());
		}
		
		return quests.toArray(new Quest[quests.size()]);
	}
	
	/**
	 * Return a table containing all QuestState to modify after a L2Attackable killing.
	 * @param npc the npc
	 * @return the quests for attacks
	 */
	public QuestState[] getQuestsForAttacks(L2NpcInstance npc)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;
		
		// Go through the QuestState of the L2PcInstance quests
		for (Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.MOBGOTATTACKED))
		{
			// Check if the Identifier of the L2Attackable attack is needed for the current quest
			if (getQuestState(quest.getName()) != null)
			{
				// Copy the current L2PcInstance QuestState in the QuestState table
				if (states == null)
				{
					states = new QuestState[]
					{
						getQuestState(quest.getName())
					};
				}
				else
				{
					states = addToQuestStateArray(states, getQuestState(quest.getName()));
				}
			}
		}
		// Return a table containing all QuestState to modify
		return states;
	}
	
	/**
	 * Return a table containing all QuestState to modify after a L2Attackable killing.
	 * @param npc the npc
	 * @return the quests for kills
	 */
	public QuestState[] getQuestsForKills(L2NpcInstance npc)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;
		
		// Go through the QuestState of the L2PcInstance quests
		for (Quest quest : npc.getTemplate().getEventQuests(Quest.QuestEventType.MOBKILLED))
		{
			// Check if the Identifier of the L2Attackable killed is needed for the current quest
			if (getQuestState(quest.getName()) != null)
			{
				// Copy the current L2PcInstance QuestState in the QuestState table
				if (states == null)
				{
					states = new QuestState[]
					{
						getQuestState(quest.getName())
					};
				}
				else
				{
					states = addToQuestStateArray(states, getQuestState(quest.getName()));
				}
			}
		}
		// Return a table containing all QuestState to modify
		return states;
	}
	
	/**
	 * Return a table containing all QuestState from the table _quests in which the L2PcInstance must talk to the NPC.
	 * @param npcId The Identifier of the NPC
	 * @return the quests for talk
	 */
	public QuestState[] getQuestsForTalk(int npcId)
	{
		// Create a QuestState table that will contain all QuestState to modify
		QuestState[] states = null;
		
		// Go through the QuestState of the L2PcInstance quests
		Quest[] quests = NpcTable.getInstance().getTemplate(npcId).getEventQuests(Quest.QuestEventType.QUEST_TALK);
		if (quests != null)
		{
			for (Quest quest : quests)
			{
				if (quest != null)
				{
					// Copy the current L2PcInstance QuestState in the QuestState table
					if (getQuestState(quest.getName()) != null)
					{
						if (states == null)
						{
							states = new QuestState[]
							{
								getQuestState(quest.getName())
							};
						}
						else
						{
							states = addToQuestStateArray(states, getQuestState(quest.getName()));
						}
					}
				}
			}
		}
		// Return a table containing all QuestState to modify
		return states;
	}
	
	/**
	 * Process quest event.
	 * @param quest the quest
	 * @param event the event
	 * @return the quest state
	 */
	public QuestState processQuestEvent(String quest, String event)
	{
		QuestState retval = null;
		if (event == null)
		{
			event = "";
		}
		if (!_quests.containsKey(quest))
		{
			return retval;
		}
		QuestState qs = getQuestState(quest);
		if ((qs == null) && (event.length() == 0))
		{
			return retval;
		}
		if (qs == null)
		{
			Quest q = QuestManager.getInstance().getQuest(quest);
			if (q == null)
			{
				return retval;
			}
			qs = q.newQuestState(this);
		}
		if (qs != null)
		{
			if (getLastQuestNpcObject() > 0)
			{
				L2Object object = L2World.getInstance().findObject(getLastQuestNpcObject());
				if ((object instanceof L2NpcInstance) && isInsideRadius(object, L2NpcInstance.INTERACTION_DISTANCE, false, false))
				{
					L2NpcInstance npc = (L2NpcInstance) object;
					QuestState[] states = getQuestsForTalk(npc.getNpcId());
					
					if (states != null)
					{
						for (QuestState state : states)
						{
							if ((state.getQuest().getQuestIntId() == qs.getQuest().getQuestIntId()) && !qs.isCompleted())
							{
								if (qs.getQuest().notifyEvent(event, npc, this))
								{
									showQuestWindow(quest, qs.getStateId());
								}
								
								retval = qs;
							}
						}
						sendPacket(new QuestList());
					}
				}
			}
		}
		return retval;
	}
	
	/**
	 * Show quest window.
	 * @param questId the quest id
	 * @param stateId the state id
	 */
	private void showQuestWindow(String questId, String stateId)
	{
		String path = "data/jscript/quests/" + questId + "/" + stateId + ".htm";
		String content = HtmCache.getInstance().getHtm(path); // TODO path for quests html
		
		if (content != null)
		{
			if (Config.DEBUG)
			{
				_log.fine("Showing quest window for quest " + questId + " state " + stateId + " html path: " + path);
			}
			
			NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
			npcReply.setHtml(content);
			sendPacket(npcReply);
		}
		
		sendPacket(new ActionFailed());
	}
	
	/**
	 * Return a table containing all L2ShortCut of the L2PcInstance.<BR>
	 * <BR>
	 * @return the all short cuts
	 */
	public L2ShortCut[] getAllShortCuts()
	{
		return _shortCuts.getAllShortCuts();
	}
	
	/**
	 * Return the L2ShortCut of the L2PcInstance corresponding to the position (page-slot)
	 * @param slot The slot in which the shortCuts is equipped
	 * @param page The page of shortCuts containing the slot
	 * @return the short cut
	 */
	public L2ShortCut getShortCut(int slot, int page)
	{
		return _shortCuts.getShortCut(slot, page);
	}
	
	/**
	 * Add a L2shortCut to the L2PcInstance _shortCuts
	 * @param shortcut the shortcut
	 */
	public void registerShortCut(L2ShortCut shortcut)
	{
		_shortCuts.registerShortCut(shortcut);
	}
	
	/**
	 * Delete the L2ShortCut corresponding to the position (page-slot) from the L2PcInstance _shortCuts
	 * @param slot the slot
	 * @param page the page
	 */
	public void deleteShortCut(int slot, int page)
	{
		_shortCuts.deleteShortCut(slot, page);
	}
	
	/**
	 * Add a L2Macro to the L2PcInstance _macroses
	 * @param macro the macro
	 */
	public void registerMacro(L2Macro macro)
	{
		_macroses.registerMacro(macro);
	}
	
	/**
	 * Delete the L2Macro corresponding to the Identifier from the L2PcInstance _macroses
	 * @param id the id
	 */
	public void deleteMacro(int id)
	{
		_macroses.deleteMacro(id);
	}
	
	/**
	 * Return all L2Macro of the L2PcInstance
	 * @return the macroses
	 */
	public MacroList getMacroses()
	{
		return _macroses;
	}
	
	/**
	 * Set the siege state of the L2PcInstance 1 = attacker, 2 = defender, 0 = not involved
	 * @param siegeState the new siege state
	 */
	public void setSiegeState(byte siegeState)
	{
		_siegeState = siegeState;
	}
	
	/**
	 * Get the siege state of the L2PcInstance 1 = attacker, 2 = defender, 0 = not involved
	 * @return the siege state
	 */
	public byte getSiegeState()
	{
		return _siegeState;
	}
	
	/**
	 * Set the PvP Flag of the L2PcInstance
	 * @param pvpFlag the new pvp flag
	 */
	public void setPvpFlag(int pvpFlag)
	{
		_pvpFlag = (byte) pvpFlag;
	}
	
	/**
	 * Gets the pvp flag.
	 * @return the pvp flag
	 */
	public byte getPvpFlag()
	{
		return _pvpFlag;
	}
	
	/**
	 * Revalidate zone.
	 * @param force the force
	 */
	public void revalidateZone(boolean force)
	{
		// Cannot validate if not in a world region (happens during teleport)
		if (getWorldRegion() == null)
		{
			return;
		}
		
		// This function is called very often from movement code
		if (force)
		{
			_zoneValidateCounter = 4;
		}
		else
		{
			_zoneValidateCounter--;
			if (_zoneValidateCounter < 0)
			{
				_zoneValidateCounter = 4;
			}
			else
			{
				return;
			}
		}
		
		getWorldRegion().revalidateZones(this);
		
		if (isInsideZone(ZONE_SIEGE))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2)
			{
				return;
			}
			_lastCompassZone = ExSetCompassZoneCode.SIEGEWARZONE2;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.SIEGEWARZONE2);
			sendPacket(cz);
		}
		else if (isInsideZone(ZONE_PVP))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.PVPZONE)
			{
				return;
			}
			_lastCompassZone = ExSetCompassZoneCode.PVPZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.PVPZONE);
			sendPacket(cz);
		}
		else if (isIn7sDungeon())
		{
			if (_lastCompassZone == ExSetCompassZoneCode.SEVENSIGNSZONE)
			{
				return;
			}
			_lastCompassZone = ExSetCompassZoneCode.SEVENSIGNSZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.SEVENSIGNSZONE);
			sendPacket(cz);
		}
		else if (isInsideZone(ZONE_PEACE))
		{
			if (_lastCompassZone == ExSetCompassZoneCode.PEACEZONE)
			{
				return;
			}
			_lastCompassZone = ExSetCompassZoneCode.PEACEZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.PEACEZONE);
			sendPacket(cz);
		}
		else
		{
			if (_lastCompassZone == ExSetCompassZoneCode.GENERALZONE)
			{
				return;
			}
			if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2)
			{
				updatePvPStatus();
			}
			_lastCompassZone = ExSetCompassZoneCode.GENERALZONE;
			ExSetCompassZoneCode cz = new ExSetCompassZoneCode(ExSetCompassZoneCode.GENERALZONE);
			sendPacket(cz);
		}
	}
	
	/**
	 * Return True if the L2PcInstance can Craft Dwarven Recipes
	 * @return true, if successful
	 */
	public boolean hasDwarvenCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN) >= 1;
	}
	
	/**
	 * Gets the dwarven craft.
	 * @return the dwarven craft
	 */
	public int getDwarvenCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN);
	}
	
	/**
	 * Return True if the L2PcInstance can Craft Dwarven Recipes
	 * @return true, if successful
	 */
	public boolean hasCommonCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_COMMON) >= 1;
	}
	
	/**
	 * Gets the common craft.
	 * @return the common craft
	 */
	public int getCommonCraft()
	{
		return getSkillLevel(L2Skill.SKILL_CREATE_COMMON);
	}
	
	/**
	 * Return the PK counter of the L2PcInstance.
	 * @return the pk kills
	 */
	public int getPkKills()
	{
		return _pkKills;
	}
	
	/**
	 * Set the PK counter of the L2PcInstance.
	 * @param pkKills the new pk kills
	 */
	public void setPkKills(int pkKills)
	{
		_pkKills = pkKills;
	}
	
	/**
	 * Return the _deleteTimer of the L2PcInstance.
	 * @return the delete timer
	 */
	public long getDeleteTimer()
	{
		return _deleteTimer;
	}
	
	/**
	 * Set the _deleteTimer of the L2PcInstance.
	 * @param deleteTimer the new delete timer
	 */
	public void setDeleteTimer(long deleteTimer)
	{
		_deleteTimer = deleteTimer;
	}
	
	/**
	 * Return the current weight of the L2PcInstance.
	 * @return the current load
	 */
	public int getCurrentLoad()
	{
		return _inventory.getTotalWeight();
	}
	
	/**
	 * Return date of las update of recomPoints.
	 * @return the last recom update
	 */
	public long getLastRecomUpdate()
	{
		return _lastRecomUpdate;
	}
	
	/**
	 * Sets the last recom update.
	 * @param date the new last recom update
	 */
	public void setLastRecomUpdate(long date)
	{
		_lastRecomUpdate = date;
	}
	
	/**
	 * @return the number of recommendation obtained by the L2PcInstance
	 */
	public int getRecomHave()
	{
		return _recomHave;
	}
	
	/**
	 * Increment the number of recommendation obtained by the L2PcInstance (Max : 255).
	 */
	protected void incRecomHave()
	{
		if (_recomHave < 255)
		{
			_recomHave++;
		}
	}
	
	/**
	 * Set the number of recommendation obtained by the L2PcInstance (Max : 255).
	 * @param value the new recommendation value
	 */
	public void setRecomHave(int value)
	{
		if (value > 255)
		{
			_recomHave = 255;
		}
		else if (value < 0)
		{
			_recomHave = 0;
		}
		else
		{
			_recomHave = value;
		}
	}
	
	/**
	 * Return the number of recommendation that the L2PcInstance can give
	 * @return the recom left
	 */
	public int getRecomLeft()
	{
		return _recomLeft;
	}
	
	/**
	 * Increment the number of recommendation that the L2PcInstance can give.
	 */
	protected void decRecomLeft()
	{
		if (_recomLeft > 0)
		{
			_recomLeft--;
		}
	}
	
	/**
	 * Give recom.
	 * @param target the target
	 */
	public void giveRecom(L2PcInstance target)
	{
		if (Config.ALT_RECOMMEND)
		{
			java.sql.Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(ADD_CHAR_RECOM);
				statement.setInt(1, getObjectId());
				statement.setInt(2, target.getObjectId());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warning("could not update char recommendations:" + e);
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (Exception e)
				{
				}
			}
		}
		target.incRecomHave();
		decRecomLeft();
		_recomChars.add(target.getObjectId());
	}
	
	/**
	 * Can recom.
	 * @param target the target
	 * @return true, if successful
	 */
	public boolean canRecom(L2PcInstance target)
	{
		return !_recomChars.contains(target.getObjectId());
	}
	
	/**
	 * Set the exp of the L2PcInstance before a death.
	 * @param exp the new exp before death
	 */
	public void setExpBeforeDeath(long exp)
	{
		_expBeforeDeath = exp;
	}
	
	/**
	 * Gets the exp before death.
	 * @return the exp before death
	 */
	public long getExpBeforeDeath()
	{
		return _expBeforeDeath;
	}
	
	/**
	 * Return the Karma of the L2PcInstance.
	 * @return the karma
	 */
	public int getKarma()
	{
		return _karma;
	}
	
	/**
	 * Set the Karma of the L2PcInstance and send a Server->Client packet StatusUpdate (broadcast).
	 * @param karma the new karma
	 */
	public void setKarma(int karma)
	{
		if (karma < 0)
		{
			karma = 0;
		}
		if ((_karma == 0) && (karma > 0))
		{
			for (L2Object object : getKnownList().getKnownObjects().values())
			{
				if ((object == null) || !(object instanceof L2GuardInstance))
				{
					continue;
				}
				
				if (((L2GuardInstance) object).getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
				{
					((L2GuardInstance) object).getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null);
				}
			}
		}
		else if ((_karma > 0) && (karma == 0))
		{
			// Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2PcInstance and all L2PcInstance to inform (broadcast)
			setKarmaFlag(0);
		}
		
		_karma = karma;
		broadcastKarma();
	}
	
	/**
	 * Return the max weight that the L2PcInstance can load.
	 * @return the max load
	 */
	public int getMaxLoad()
	{
		// Weight Limit = (CON Modifier*69000)*Skills
		// Source http://l2p.bravehost.com/weightlimit.html (May 2007)
		// Fitted exponential curve to the data
		int con = getCON();
		if (con < 1)
		{
			return 31000;
		}
		if (con > 59)
		{
			return 176000;
		}
		double baseLoad = Math.pow(1.029993928, con) * 30495.627366;
		return (int) calcStat(Stats.MAX_LOAD, baseLoad * Config.ALT_WEIGHT_LIMIT, this, null);
	}
	
	/**
	 * Gets the expertise penalty.
	 * @return the expertise penalty
	 */
	public int getExpertisePenalty()
	{
		return _expertisePenalty;
	}
	
	/**
	 * Gets the weight penalty.
	 * @return the weight penalty
	 */
	public int getWeightPenalty()
	{
		if (_dietMode)
		{
			return 0;
		}
		return _curWeightPenalty;
	}
	
	/**
	 * Update the overloaded status of the L2PcInstance.
	 */
	public void refreshOverloaded()
	{
		int maxLoad = getMaxLoad();
		if (maxLoad > 0)
		{
			setIsOverloaded(getCurrentLoad() > maxLoad);
			int weightproc = (getCurrentLoad() * 1000) / maxLoad;
			int newWeightPenalty;
			if ((weightproc < 500) || _dietMode)
			{
				newWeightPenalty = 0;
			}
			else if (weightproc < 666)
			{
				newWeightPenalty = 1;
			}
			else if (weightproc < 800)
			{
				newWeightPenalty = 2;
			}
			else if (weightproc < 1000)
			{
				newWeightPenalty = 3;
			}
			else
			{
				newWeightPenalty = 4;
			}
			
			if (_curWeightPenalty != newWeightPenalty)
			{
				_curWeightPenalty = newWeightPenalty;
				if ((newWeightPenalty > 0) && !_dietMode)
				{
					super.addSkill(SkillTable.getInstance().getInfo(4270, newWeightPenalty));
				}
				else
				{
					super.removeSkill(getKnownSkill(4270));
				}
				
				sendPacket(new EtcStatusUpdate(this));
				Broadcast.toKnownPlayers(this, new CharInfo(this));
			}
		}
	}
	
	/**
	 * Refresh expertise penalty.
	 */
	public void refreshExpertisePenalty()
	{
		int newPenalty = 0;
		
		for (L2ItemInstance item : getInventory().getItems())
		{
			if ((item != null) && item.isEquipped())
			{
				int crystaltype = item.getItem().getCrystalType();
				
				if (crystaltype > newPenalty)
				{
					newPenalty = crystaltype;
				}
			}
		}
		
		newPenalty = newPenalty - getExpertiseIndex();
		
		if (newPenalty <= 0)
		{
			newPenalty = 0;
		}
		
		if (getExpertisePenalty() != newPenalty)
		{
			_expertisePenalty = newPenalty;
			
			if (newPenalty > 0)
			{
				super.addSkill(SkillTable.getInstance().getInfo(4267, 1)); // level used to be newPenalty
			}
			else
			{
				super.removeSkill(getKnownSkill(4267));
			}
			
			sendPacket(new EtcStatusUpdate(this));
		}
	}
	
	/**
	 * Check if weapon is allowed.
	 */
	public void checkIfWeaponIsAllowed()
	{
		// Override for Gamemasters
		if (isGM())
		{
			return;
		}
		
		// Iterate through all effects currently on the character.
		for (L2Effect currenteffect : getAllEffects())
		{
			L2Skill effectSkill = currenteffect.getSkill();
			
			// Ignore all buff skills that are party related (ie. songs, dances) while still remaining weapon dependant on cast though.
			if (!effectSkill.isOffensive() && !((effectSkill.getTargetType() == SkillTargetType.TARGET_PARTY) && (effectSkill.getSkillType() == SkillType.BUFF)))
			{
				// Check to rest to assure current effect meets weapon requirements.
				if (!effectSkill.getWeaponDependancy(this))
				{
					sendMessage(effectSkill.getName() + " cannot be used with this weapon.");
					
					if (Config.DEBUG)
					{
						_log.info("   | Skill " + effectSkill.getName() + " has been disabled for (" + getName() + "); Reason: Incompatible Weapon Type.");
					}
					
					currenteffect.exit();
				}
			}
			
			continue;
		}
	}
	
	/**
	 * Check ss match.
	 * @param equipped the equipped
	 * @param unequipped the unequipped
	 */
	public void checkSSMatch(L2ItemInstance equipped, L2ItemInstance unequipped)
	{
		if (unequipped == null)
		{
			return;
		}
		
		if ((unequipped.getItem().getType2() == L2Item.TYPE2_WEAPON) && (equipped == null ? true : equipped.getItem().getCrystalType() != unequipped.getItem().getCrystalType()))
		{
			for (L2ItemInstance ss : getInventory().getItems())
			{
				int _itemId = ss.getItemId();
				
				if ((((_itemId >= 2509) && (_itemId <= 2514)) || ((_itemId >= 3947) && (_itemId <= 3952)) || ((_itemId <= 1804) && (_itemId >= 1808)) || (_itemId == 5789) || (_itemId == 5790) || (_itemId == 1835)) && (ss.getItem().getCrystalType() == unequipped.getItem().getCrystalType()))
				{
					sendPacket(new ExAutoSoulShot(_itemId, 0));
					
					SystemMessage sm = new SystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED);
					sm.addString(ss.getItemName());
					sendPacket(sm);
				}
			}
		}
	}
	
	/**
	 * Return the the PvP Kills of the L2PcInstance (Number of player killed during a PvP).
	 * @return the pvp kills
	 */
	public int getPvpKills()
	{
		return _pvpKills;
	}
	
	/**
	 * Set the the PvP Kills of the L2PcInstance (Number of player killed during a PvP).
	 * @param pvpKills the new pvp kills
	 */
	public void setPvpKills(int pvpKills)
	{
		_pvpKills = pvpKills;
	}
	
	/**
	 * Return the ClassId object of the L2PcInstance contained in L2PcTemplate.
	 * @return the class id
	 */
	public ClassId getClassId()
	{
		return getTemplate().classId;
	}
	
	/**
	 * Set the template of the L2PcInstance.
	 * @param Id The Identifier of the L2PcTemplate to set to the L2PcInstance
	 */
	public void setClassId(int Id)
	{
		
		if ((getLvlJoinedAcademy() != 0) && (_clan != null) && (PlayerClass.values()[Id].getLevel() == ClassLevel.Third))
		{
			if (getLvlJoinedAcademy() <= 16)
			{
				_clan.setReputationScore(_clan.getReputationScore() + 400, true);
			}
			else if (getLvlJoinedAcademy() >= 39)
			{
				_clan.setReputationScore(_clan.getReputationScore() + 170, true);
			}
			else
			{
				_clan.setReputationScore(_clan.getReputationScore() + (400 - ((getLvlJoinedAcademy() - 16) * 10)), true);
			}
			_clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(_clan));
			setLvlJoinedAcademy(0);
			// oust pledge member from the academy, cuz he has finished his 2nd class transfer
			SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_EXPELLED);
			msg.addString(getName());
			_clan.broadcastToOnlineMembers(msg);
			_clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(getName()));
			_clan.removeClanMember(getName(), 0);
			sendPacket(new SystemMessage(SystemMessageId.ACADEMY_MEMBERSHIP_TERMINATED));
			
			// receive graduation gift
			getInventory().addItem("Gift", 8181, 1, this, null); // give academy circlet
			getInventory().updateDatabase(); // update database
		}
		if (isSubClassActive())
		{
			getSubClasses().get(_classIndex).setClassId(Id);
		}
		doCast(SkillTable.getInstance().getInfo(5103, 1));
		setClassTemplate(Id);
	}
	
	/**
	 * Return the Experience of the L2PcInstance.
	 * @return the exp
	 */
	public long getExp()
	{
		return getStat().getExp();
	}
	
	/**
	 * Sets the active enchant item.
	 * @param scroll the new active enchant item
	 */
	public void setActiveEnchantItem(L2ItemInstance scroll)
	{
		_activeEnchantItem = scroll;
	}
	
	/**
	 * Gets the active enchant item.
	 * @return the active enchant item
	 */
	public L2ItemInstance getActiveEnchantItem()
	{
		return _activeEnchantItem;
	}
	
	/**
	 * Set the fists weapon of the L2PcInstance (used when no weapon is equiped).
	 * @param weaponItem The fists L2Weapon to set to the L2PcInstance
	 */
	public void setFistsWeaponItem(L2Weapon weaponItem)
	{
		_fistsWeaponItem = weaponItem;
	}
	
	/**
	 * Return the fists weapon of the L2PcInstance (used when no weapon is equiped).
	 * @return the fists weapon item
	 */
	public L2Weapon getFistsWeaponItem()
	{
		return _fistsWeaponItem;
	}
	
	/**
	 * Return the fists weapon of the L2PcInstance Class (used when no weapon is equiped).
	 * @param classId the class id
	 * @return the l2 weapon
	 */
	public L2Weapon findFistsWeaponItem(int classId)
	{
		L2Weapon weaponItem = null;
		if ((classId >= 0x00) && (classId <= 0x09))
		{
			// human fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(246);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x0a) && (classId <= 0x11))
		{
			// human mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(251);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x12) && (classId <= 0x18))
		{
			// elven fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(244);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x19) && (classId <= 0x1e))
		{
			// elven mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(249);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x1f) && (classId <= 0x25))
		{
			// dark elven fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(245);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x26) && (classId <= 0x2b))
		{
			// dark elven mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(250);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x2c) && (classId <= 0x30))
		{
			// orc fighter fists
			L2Item temp = ItemTable.getInstance().getTemplate(248);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x31) && (classId <= 0x34))
		{
			// orc mage fists
			L2Item temp = ItemTable.getInstance().getTemplate(252);
			weaponItem = (L2Weapon) temp;
		}
		else if ((classId >= 0x35) && (classId <= 0x39))
		{
			// dwarven fists
			L2Item temp = ItemTable.getInstance().getTemplate(247);
			weaponItem = (L2Weapon) temp;
		}
		
		return weaponItem;
	}
	
	/**
	 * Give Expertise skill of this level and remove beginner Lucky skill. <B><U> Actions</U> :</B><BR>
	 * <li>Get the Level of the L2PcInstance</li> <li>If L2PcInstance Level is 5, remove beginner Lucky skill</li> <li>Add the Expertise skill corresponding to its Expertise level</li> <li>Update the overloaded status of the L2PcInstance</li><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T give other free skills (SP needed = 0)</B></FONT>
	 */
	public void rewardSkills()
	{
		// Get the Level of the L2PcInstance
		int lvl = getLevel();
		
		// Remove beginner Lucky skill
		if (lvl == 10)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(194, 1);
			skill = removeSkill(skill);
			
			if (Config.DEBUG && (skill != null))
			{
				_log.fine("removed skill 'Lucky' from " + getName());
			}
		}
		
		// Calculate the current higher Expertise of the L2PcInstance
		for (int i = 0; i < EXPERTISE_LEVELS.length; i++)
		{
			if (lvl >= EXPERTISE_LEVELS[i])
			{
				setExpertiseIndex(i);
			}
		}
		
		// Add the Expertise skill corresponding to its Expertise level
		if (getExpertiseIndex() > 0)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(239, getExpertiseIndex());
			addSkill(skill, true);
			
			if (Config.DEBUG)
			{
				_log.fine("awarded " + getName() + " with new expertise.");
			}
			
		}
		else
		{
			if (Config.DEBUG)
			{
				_log.fine("No skills awarded at lvl: " + lvl);
			}
		}
		
		// Active skill dwarven craft
		
		if ((getSkillLevel(1321) < 1) && (getRace() == Race.dwarf))
		{
			L2Skill skill = SkillTable.getInstance().getInfo(1321, 1);
			addSkill(skill, true);
		}
		
		// Active skill common craft
		if (getSkillLevel(1322) < 1)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(1322, 1);
			addSkill(skill, true);
		}
		
		for (int i = 0; i < COMMON_CRAFT_LEVELS.length; i++)
		{
			if ((lvl >= COMMON_CRAFT_LEVELS[i]) && (getSkillLevel(1320) < (i + 1)))
			{
				L2Skill skill = SkillTable.getInstance().getInfo(1320, (i + 1));
				addSkill(skill, true);
			}
		}
		
		// Auto-Learn skills if activated
		if (Config.AUTO_LEARN_SKILLS)
		{
			giveAvailableSkills();
		}
		sendSkillList();
		// This function gets called on login, so not such a bad place to check weight
		refreshOverloaded(); // Update the overloaded status of the L2PcInstance
		refreshExpertisePenalty(); // Update the expertise status of the L2PcInstance
	}
	
	/**
	 * Regive all skills which aren't saved to database, like Noble, Hero, Clan Skills .
	 */
	private void regiveTemporarySkills()
	{
		// Do not call this on enterworld or char load
		
		// Add noble skills if noble
		if (isNoble())
		{
			setNoble(true);
		}
		
		// Add Hero skills if hero
		if (isHero())
		{
			setHero(true);
		}
		
		// Add clan skills
		if ((getClan() != null) && (getClan().getReputationScore() >= 0))
		{
			L2Skill[] skills = getClan().getAllSkills();
			for (L2Skill sk : skills)
			{
				if (sk.getMinPledgeClass() <= getPledgeClass())
				{
					addSkill(sk, false);
				}
			}
		}
		
		// Reload passive skills from armors / jewels / weapons
		getInventory().reloadEquippedItems();
		
	}
	
	/**
	 * Give all available skills to the player.<br>
	 * <br>
	 */
	private void giveAvailableSkills()
	{
		int unLearnable = 0;
		int skillCounter = 0;
		
		// Get available skills
		L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableSkills(this, getClassId());
		while (skills.length > unLearnable)
		{
			for (L2SkillLearn s : skills)
			{
				L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
				if ((sk == null) || !sk.getCanLearn(getClassId()))
				{
					unLearnable++;
					continue;
				}
				
				if (getSkillLevel(sk.getId()) == -1)
				{
					skillCounter++;
				}
				
				addSkill(sk, true);
			}
			
			// Get new available skills
			skills = SkillTreeTable.getInstance().getAvailableSkills(this, getClassId());
		}
		
		sendMessage("You have learned " + skillCounter + " new skills.");
	}
	
	/**
	 * Set the Experience value of the L2PcInstance.
	 * @param exp the new exp
	 */
	public void setExp(long exp)
	{
		getStat().setExp(exp);
	}
	
	/**
	 * Return the Race object of the L2PcInstance.
	 * @return the race
	 */
	public Race getRace()
	{
		if (!isSubClassActive())
		{
			return getTemplate().race;
		}
		
		L2PcTemplate charTemp = CharTemplateTable.getInstance().getTemplate(_baseClass);
		return charTemp.race;
	}
	
	/**
	 * Gets the radar.
	 * @return the radar
	 */
	public L2Radar getRadar()
	{
		return _radar;
	}
	
	/**
	 * Return the SP amount of the L2PcInstance.
	 * @return the sp
	 */
	public int getSp()
	{
		return getStat().getSp();
	}
	
	/**
	 * Set the SP amount of the L2PcInstance.
	 * @param sp the new sp
	 */
	public void setSp(int sp)
	{
		super.getStat().setSp(sp);
	}
	
	/**
	 * Return true if this L2PcInstance is a clan leader in ownership of the passed castle.
	 * @param castleId the castle id
	 * @return true, if is castle lord
	 */
	public boolean isCastleLord(int castleId)
	{
		L2Clan clan = getClan();
		
		// player has clan and is the clan leader, check the castle info
		if ((clan != null) && (clan.getLeader().getPlayerInstance() == this))
		{
			// if the clan has a castle and it is actually the queried castle, return true
			Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
			if ((castle != null) && (castle == CastleManager.getInstance().getCastleById(castleId)))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Return the Clan Identifier of the L2PcInstance.
	 * @return the clan id
	 */
	public int getClanId()
	{
		return _clanId;
	}
	
	/**
	 * Return the Clan Crest Identifier of the L2PcInstance or 0.
	 * @return the clan crest id
	 */
	public int getClanCrestId()
	{
		if ((_clan != null) && _clan.hasCrest())
		{
			return _clan.getCrestId();
		}
		
		return 0;
	}
	
	/**
	 * Gets the clan crest large id.
	 * @return The Clan CrestLarge Identifier or 0
	 */
	public int getClanCrestLargeId()
	{
		if ((_clan != null) && _clan.hasCrestLarge())
		{
			return _clan.getCrestLargeId();
		}
		
		return 0;
	}
	
	/**
	 * Gets the clan join expiry time.
	 * @return the clan join expiry time
	 */
	public long getClanJoinExpiryTime()
	{
		return _clanJoinExpiryTime;
	}
	
	/**
	 * Sets the clan join expiry time.
	 * @param time the new clan join expiry time
	 */
	public void setClanJoinExpiryTime(long time)
	{
		_clanJoinExpiryTime = time;
	}
	
	/**
	 * Gets the clan create expiry time.
	 * @return the clan create expiry time
	 */
	public long getClanCreateExpiryTime()
	{
		return _clanCreateExpiryTime;
	}
	
	/**
	 * Sets the clan create expiry time.
	 * @param time the new clan create expiry time
	 */
	public void setClanCreateExpiryTime(long time)
	{
		_clanCreateExpiryTime = time;
	}
	
	/**
	 * Sets the online time.
	 * @param time the new online time
	 */
	public void setOnlineTime(long time)
	{
		_onlineTime = time;
		_onlineBeginTime = System.currentTimeMillis();
	}
	
	/**
	 * Return the PcInventory Inventory of the L2PcInstance contained in _inventory.
	 * @return the inventory
	 */
	public PcInventory getInventory()
	{
		return _inventory;
	}
	
	/**
	 * Delete a ShortCut of the L2PcInstance _shortCuts.
	 * @param objectId the object id
	 */
	public void removeItemFromShortCut(int objectId)
	{
		_shortCuts.deleteShortCutByObjectId(objectId);
	}
	
	/**
	 * Return True if the L2PcInstance is sitting.
	 * @return true, if is sitting
	 */
	public boolean isSitting()
	{
		return _waitTypeSitting;
	}
	
	/**
	 * Set _waitTypeSitting to given value.
	 * @param state the new checks if is sitting
	 */
	public void setIsSitting(boolean state)
	{
		_waitTypeSitting = state;
	}
	
	/**
	 * Sit down the L2PcInstance, set the AI Intention to AI_INTENTION_REST and send a Server->Client ChangeWaitType packet (broadcast) .
	 */
	public void sitDown()
	{
		if (isCastingNow() && !_relax)
		{
			sendMessage("Cannot sit while casting");
			return;
		}
		
		if (!_waitTypeSitting && !isAttackingDisabled() && !isOutOfControl() && !isImobilised())
		{
			breakAttack();
			setIsSitting(true);
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_SITTING));
			// Schedule a sit down task to wait for the animation to finish
			ThreadPoolManager.getInstance().scheduleGeneral(new SitDownTask(this), 2500);
			setIsParalyzed(true);
		}
	}
	
	/**
	 * Sit down Task.
	 */
	class SitDownTask implements Runnable
	{
		
		/** The _player. */
		L2PcInstance _player;
		
		/**
		 * Instantiates a new sit down task.
		 * @param player the player
		 */
		SitDownTask(L2PcInstance player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			_player.setIsParalyzed(false);
			_player.getAI().setIntention(CtrlIntention.AI_INTENTION_REST);
		}
	}
	
	/**
	 * Stand up Task.
	 */
	class StandUpTask implements Runnable
	{
		
		/** The _player. */
		L2PcInstance _player;
		
		/**
		 * Instantiates a new stand up task.
		 * @param player the player
		 */
		StandUpTask(L2PcInstance player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			_player.setIsSitting(false);
			_player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
	}
	
	/**
	 * Stand up the L2PcInstance, set the AI Intention to AI_INTENTION_IDLE and send a Server->Client ChangeWaitType packet (broadcast) .
	 */
	public void standUp()
	{
		if (L2Event.active && eventSitForced)
		{
			sendMessage("A dark force beyond your mortal understanding makes your knees to shake when you try to stand up ...");
		}
		else if (_waitTypeSitting && !isInStoreMode() && !isAlikeDead())
		{
			if (_relax)
			{
				setRelax(false);
				stopEffects(L2Effect.EffectType.RELAXING);
			}
			
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STANDING));
			// Schedule a stand up task to wait for the animation to finish
			ThreadPoolManager.getInstance().scheduleGeneral(new StandUpTask(this), 2500);
		}
	}
	
	/**
	 * Set the value of the _relax value. Must be True if using skill Relax and False if not.
	 * @param val the new relax
	 */
	public void setRelax(boolean val)
	{
		_relax = val;
	}
	
	/**
	 * Return the PcWarehouse object of the L2PcInstance.
	 * @return the warehouse
	 */
	public PcWarehouse getWarehouse()
	{
		if (_warehouse == null)
		{
			_warehouse = new PcWarehouse(this);
			_warehouse.restore();
		}
		if (Config.WAREHOUSE_CACHE)
		{
			WarehouseCacheManager.getInstance().addCacheTask(this);
		}
		return _warehouse;
	}
	
	/**
	 * Free memory used by Warehouse.
	 */
	public void clearWarehouse()
	{
		if (_warehouse != null)
		{
			_warehouse.deleteMe();
		}
		_warehouse = null;
	}
	
	/**
	 * Return the PcFreight object of the L2PcInstance.
	 * @return the freight
	 */
	public PcFreight getFreight()
	{
		return _freight;
	}
	
	/**
	 * Return the Identifier of the L2PcInstance.
	 * @return the char id
	 */
	public int getCharId()
	{
		return _charId;
	}
	
	/**
	 * Set the Identifier of the L2PcInstance.
	 * @param charId the new char id
	 */
	public void setCharId(int charId)
	{
		_charId = charId;
	}
	
	/**
	 * Return the Adena amount of the L2PcInstance.
	 * @return the adena
	 */
	public int getAdena()
	{
		return _inventory.getAdena();
	}
	
	/**
	 * Return the Ancient Adena amount of the L2PcInstance.
	 * @return the ancient adena
	 */
	public int getAncientAdena()
	{
		return _inventory.getAncientAdena();
	}
	
	/**
	 * Add adena to Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ADENA);
			sm.addNumber(count);
			sendPacket(sm);
		}
		
		if (count > 0)
		{
			_inventory.addAdena(process, count, this, reference);
			
			// Send update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(_inventory.getAdenaInstance());
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
		}
	}
	
	/**
	 * Reduce adena in Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be reduced
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean reduceAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (count > getAdena())
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			}
			return false;
		}
		
		if (count > 0)
		{
			L2ItemInstance adenaItem = _inventory.getAdenaInstance();
			_inventory.reduceAdena(process, count, this, reference);
			
			// Send update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(adenaItem);
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
			
			if (sendMessage)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.DISSAPEARED_ADENA);
				sm.addNumber(count);
				sendPacket(sm);
			}
		}
		
		return true;
	}
	
	/**
	 * Add ancient adena to Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of ancient adena to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addAncientAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
			sm.addItemName(PcInventory.ANCIENT_ADENA_ID);
			sm.addNumber(count);
			sendPacket(sm);
		}
		
		if (count > 0)
		{
			_inventory.addAncientAdena(process, count, this, reference);
			
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(_inventory.getAncientAdenaInstance());
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
		}
	}
	
	/**
	 * Reduce ancient adena in Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of ancient adena to be reduced
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean reduceAncientAdena(String process, int count, L2Object reference, boolean sendMessage)
	{
		if (count > getAncientAdena())
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			}
			
			return false;
		}
		
		if (count > 0)
		{
			L2ItemInstance ancientAdenaItem = _inventory.getAncientAdenaInstance();
			_inventory.reduceAncientAdena(process, count, this, reference);
			
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(ancientAdenaItem);
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
			
			if (sendMessage)
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.DISSAPEARED_ITEM);
				sm.addNumber(count);
				sm.addItemName(PcInventory.ANCIENT_ADENA_ID);
				sendPacket(sm);
			}
		}
		
		return true;
	}
	
	/**
	 * Adds item to inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		if (item.getCount() > 0)
		{
			// Sends message to client if requested
			if (sendMessage)
			{
				if (item.getCount() > 1)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
					sm.addItemName(item.getItemId());
					sm.addNumber(item.getCount());
					sendPacket(sm);
				}
				else if (item.getEnchantLevel() > 0)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_A_S1_S2);
					sm.addNumber(item.getEnchantLevel());
					sm.addItemName(item.getItemId());
					sendPacket(sm);
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1);
					sm.addItemName(item.getItemId());
					sendPacket(sm);
				}
			}
			
			// Add the item to inventory
			L2ItemInstance newitem = _inventory.addItem(process, item, this, reference);
			
			// Send inventory update packet
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate playerIU = new InventoryUpdate();
				playerIU.addItem(newitem);
				sendPacket(playerIU);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
			
			// Update current load as well
			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
			sendPacket(su);
			
			// Cursed Weapon
			if (CursedWeaponsManager.getInstance().isCursed(newitem.getItemId()))
			{
				CursedWeaponsManager.getInstance().activate(this, newitem);
			}
			
			// If over capacity, trop the item
			if (!isGM() && !_inventory.validateCapacity(0))
			{
				dropItem("InvDrop", newitem, null, true);
			}
		}
	}
	
	/**
	 * Adds item to Inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be added
	 * @param count : int Quantity of items to be added
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addItem(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		if (count > 0)
		{
			// Sends message to client if requested
			if (sendMessage && ((!isCastingNow() && (ItemTable.getInstance().createDummyItem(itemId).getItemType() == L2EtcItemType.HERB)) || (ItemTable.getInstance().createDummyItem(itemId).getItemType() != L2EtcItemType.HERB)))
			{
				if (count > 1)
				{
					if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
						sm.addItemName(itemId);
						sm.addNumber(count);
						sendPacket(sm);
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1_S2);
						sm.addItemName(itemId);
						sm.addNumber(count);
						sendPacket(sm);
					}
				}
				else
				{
					if (process.equalsIgnoreCase("sweep") || process.equalsIgnoreCase("Quest"))
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
						sm.addItemName(itemId);
						sendPacket(sm);
					}
					else
					{
						SystemMessage sm = new SystemMessage(SystemMessageId.YOU_PICKED_UP_S1);
						sm.addItemName(itemId);
						sendPacket(sm);
					}
				}
			}
			// Auto use herbs - autoloot
			if (ItemTable.getInstance().createDummyItem(itemId).getItemType() == L2EtcItemType.HERB) // If item is herb dont add it to iv :]
			{
				if (!isCastingNow())
				{
					L2ItemInstance herb = new L2ItemInstance(_charId, itemId);
					IItemHandler handler = ItemHandler.getInstance().getItemHandler(herb.getItemId());
					if (handler == null)
					{
						_log.warning("No item handler registered for Herb - item ID " + herb.getItemId() + ".");
					}
					else
					{
						handler.useItem(this, herb);
						if (_herbstask >= 100)
						{
							_herbstask -= 100;
						}
					}
				}
				else
				{
					_herbstask += 100;
					ThreadPoolManager.getInstance().scheduleAi(new HerbTask(process, itemId, count, reference, sendMessage), _herbstask);
				}
			}
			else
			{
				// Add the item to inventory
				L2ItemInstance item = _inventory.addItem(process, itemId, count, this, reference);
				
				// Send inventory update packet
				if (!Config.FORCE_INVENTORY_UPDATE)
				{
					InventoryUpdate playerIU = new InventoryUpdate();
					playerIU.addItem(item);
					sendPacket(playerIU);
				}
				else
				{
					sendPacket(new ItemList(this, false));
				}
				
				// Update current load as well
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
				sendPacket(su);
				
				// Cursed Weapon
				if (CursedWeaponsManager.getInstance().isCursed(item.getItemId()))
				{
					CursedWeaponsManager.getInstance().activate(this, item);
				}
				
				// If over capacity, drop the item
				if (!isGM() && !_inventory.validateCapacity(0))
				{
					dropItem("InvDrop", item, null, true);
				}
			}
		}
	}
	
	/**
	 * Destroy item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		int oldCount = item.getCount();
		item = _inventory.destroyItem(process, item, this, reference);
		
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}
			
			return false;
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.DISSAPEARED_ITEM);
			sm.addNumber(oldCount);
			sm.addItemName(item.getItemId());
			sendPacket(sm);
		}
		
		return true;
	}
	
	/**
	 * Destroys item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	@Override
	public boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = _inventory.getItemByObjectId(objectId);
		
		if ((item == null) || (item.getCount() < count) || (_inventory.destroyItem(process, objectId, count, this, reference) == null))
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}
			
			return false;
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.DISSAPEARED_ITEM);
			sm.addNumber(count);
			sm.addItemName(item.getItemId());
			sendPacket(sm);
		}
		
		return true;
	}
	
	/**
	 * Destroys shots from inventory without logging and only occasional saving to database. Sends a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItemWithoutTrace(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = _inventory.getItemByObjectId(objectId);
		
		if ((item == null) || (item.getCount() < count))
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}
			return false;
		}
		
		// Adjust item quantity
		if (item.getCount() > count)
		{
			synchronized (item)
			{
				item.changeCountWithoutTrace(process, -count, this, reference);
				item.setLastChange(L2ItemInstance.MODIFIED);
				
				// could do also without saving, but let's save approx 1 of 10
				if ((GameTimeController.getGameTicks() % 10) == 0)
				{
					item.updateDatabase();
				}
				_inventory.refreshWeight();
			}
		}
		else
		{
			// Destroy entire item and save to database
			_inventory.destroyItem(process, item, this, reference);
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.DISSAPEARED_ITEM);
			sm.addNumber(count);
			sm.addItemName(item.getItemId());
			sendPacket(sm);
		}
		
		return true;
	}
	
	/**
	 * Destroy item from inventory by using its <B>itemId</B> and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	@Override
	public boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = _inventory.getItemByItemId(itemId);
		
		if ((item == null) || (item.getCount() < count) || (_inventory.destroyItemByItemId(process, itemId, count, this, reference) == null))
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}
			
			return false;
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.DISSAPEARED_ITEM);
			sm.addNumber(count);
			sm.addItemName(itemId);
			sendPacket(sm);
		}
		
		return true;
	}
	
	/**
	 * Destroy all weared items from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void destroyWearedItems(String process, L2Object reference, boolean sendMessage)
	{
		
		// Go through all Items of the inventory
		for (L2ItemInstance item : getInventory().getItems())
		{
			// Check if the item is a Try On item in order to remove it
			if (item.isWear())
			{
				if (item.isEquipped())
				{
					getInventory().unEquipItemInSlotAndRecord(item.getEquipSlot());
				}
				
				if (_inventory.destroyItem(process, item, this, reference) == null)
				{
					_log.warning("Player " + getName() + " can't destroy weared item: " + item.getName() + "[ " + item.getObjectId() + " ]");
					continue;
				}
				
				// Send an Unequipped Message in system window of the player for each Item
				SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISARMED);
				sm.addItemName(item.getItemId());
				sendPacket(sm);
				
			}
		}
		
		// Send the StatusUpdate Server->Client Packet to the player with new CUR_LOAD (0x0e) information
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Send the ItemList Server->Client Packet to the player in order to refresh its Inventory
		ItemList il = new ItemList(getInventory().getItems(), true);
		sendPacket(il);
		
		// Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers
		broadcastUserInfo();
		
		// Sends message to client if requested
		sendMessage("Trying-on mode has ended.");
		
	}
	
	/**
	 * Transfers item to another ItemContainer and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId the object id
	 * @param count : int Quantity of items to be transfered
	 * @param target the target
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance transferItem(String process, int objectId, int count, Inventory target, L2Object reference)
	{
		L2ItemInstance oldItem = checkItemManipulation(objectId, count, "transfer");
		if (oldItem == null)
		{
			return null;
		}
		L2ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, this, reference);
		if (newItem == null)
		{
			return null;
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			
			if ((oldItem.getCount() > 0) && (oldItem != newItem))
			{
				playerIU.addModifiedItem(oldItem);
			}
			else
			{
				playerIU.addRemovedItem(oldItem);
			}
			
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		StatusUpdate playerSU = new StatusUpdate(getObjectId());
		playerSU.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(playerSU);
		
		// Send target update packet
		if (target instanceof PcInventory)
		{
			L2PcInstance targetPlayer = ((PcInventory) target).getOwner();
			
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate playerIU = new InventoryUpdate();
				
				if (newItem.getCount() > count)
				{
					playerIU.addModifiedItem(newItem);
				}
				else
				{
					playerIU.addNewItem(newItem);
				}
				
				targetPlayer.sendPacket(playerIU);
			}
			else
			{
				targetPlayer.sendPacket(new ItemList(targetPlayer, false));
			}
			
			// Update current load as well
			playerSU = new StatusUpdate(targetPlayer.getObjectId());
			playerSU.addAttribute(StatusUpdate.CUR_LOAD, targetPlayer.getCurrentLoad());
			targetPlayer.sendPacket(playerSU);
		}
		else if (target instanceof PetInventory)
		{
			PetInventoryUpdate petIU = new PetInventoryUpdate();
			
			if (newItem.getCount() > count)
			{
				petIU.addModifiedItem(newItem);
			}
			else
			{
				petIU.addNewItem(newItem);
			}
			
			((PetInventory) target).getOwner().getOwner().sendPacket(petIU);
		}
		
		return newItem;
	}
	
	/**
	 * Drop item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param item : L2ItemInstance to be dropped
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean dropItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		item = _inventory.dropItem(process, item, this, reference);
		
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}
			
			return false;
		}
		
		item.dropMe(this, (getClientX() + Rnd.get(50)) - 25, (getClientY() + Rnd.get(50)) - 25, getClientZ() + 20);
		
		if ((Config.AUTODESTROY_ITEM_AFTER > 0) && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
		{
			if ((item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable())
			{
				ItemsAutoDestroy.getInstance().addItem(item);
			}
		}
		if (Config.DESTROY_DROPPED_PLAYER_ITEM)
		{
			if (!item.isEquipable() || (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM))
			{
				item.setProtected(false);
			}
			else
			{
				item.setProtected(true);
			}
		}
		else
		{
			item.setProtected(true);
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(item);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DROPPED_S1);
			sm.addItemName(item.getItemId());
			sendPacket(sm);
		}
		
		return true;
	}
	
	/**
	 * Drop item from inventory by using its <B>objectID</B> and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be dropped
	 * @param count : int Quantity of items to be dropped
	 * @param x : int coordinate for drop X
	 * @param y : int coordinate for drop Y
	 * @param z : int coordinate for drop Z
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance dropItem(String process, int objectId, int count, int x, int y, int z, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance invitem = _inventory.getItemByObjectId(objectId);
		L2ItemInstance item = _inventory.dropItem(process, objectId, count, this, reference);
		
		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			}
			
			return null;
		}
		
		item.dropMe(this, x, y, z);
		
		if ((Config.AUTODESTROY_ITEM_AFTER > 0) && Config.DESTROY_DROPPED_PLAYER_ITEM && !Config.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
		{
			if ((item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM) || !item.isEquipable())
			{
				ItemsAutoDestroy.getInstance().addItem(item);
			}
		}
		if (Config.DESTROY_DROPPED_PLAYER_ITEM)
		{
			if (!item.isEquipable() || (item.isEquipable() && Config.DESTROY_EQUIPABLE_PLAYER_ITEM))
			{
				item.setProtected(false);
			}
			else
			{
				item.setProtected(true);
			}
		}
		else
		{
			item.setProtected(true);
		}
		
		// Send inventory update packet
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate playerIU = new InventoryUpdate();
			playerIU.addItem(invitem);
			sendPacket(playerIU);
		}
		else
		{
			sendPacket(new ItemList(this, false));
		}
		
		// Update current load as well
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
		sendPacket(su);
		
		// Sends message to client if requested
		if (sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DROPPED_S1);
			sm.addItemName(item.getItemId());
			sendPacket(sm);
		}
		
		return item;
	}
	
	/**
	 * Check item manipulation.
	 * @param objectId the object id
	 * @param count the count
	 * @param action the action
	 * @return the l2 item instance
	 */
	public L2ItemInstance checkItemManipulation(int objectId, int count, String action)
	{
		// TODO: if we remove objects that are not visisble from the L2World, we'll have to remove this check
		if (L2World.getInstance().findObject(objectId) == null)
		{
			_log.finest(getObjectId() + ": player tried to " + action + " item not available in L2World");
			return null;
		}
		
		L2ItemInstance item = getInventory().getItemByObjectId(objectId);
		
		if ((item == null) || (item.getOwnerId() != getObjectId()))
		{
			_log.finest(getObjectId() + ": player tried to " + action + " item he is not owner of");
			return null;
		}
		
		if ((count < 0) || ((count > 1) && !item.isStackable()))
		{
			_log.finest(getObjectId() + ": player tried to " + action + " item with invalid count: " + count);
			return null;
		}
		
		if (count > item.getCount())
		{
			_log.finest(getObjectId() + ": player tried to " + action + " more items than he owns");
			return null;
		}
		
		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if (((getPet() != null) && (getPet().getControlItemId() == objectId)) || (getMountObjectID() == objectId))
		{
			if (Config.DEBUG)
			{
				_log.finest(getObjectId() + ": player tried to " + action + " item controling pet");
			}
			
			return null;
		}
		
		if ((getActiveEnchantItem() != null) && (getActiveEnchantItem().getObjectId() == objectId))
		{
			if (Config.DEBUG)
			{
				_log.finest(getObjectId() + ":player tried to " + action + " an enchant scroll he was using");
			}
			
			return null;
		}
		
		if (item.isWear())
		{
			// cannot drop/trade wear-items
			return null;
		}
		
		return item;
	}
	
	/**
	 * Set _protectEndTime according settings.
	 * @param protect the new protection
	 */
	public void setProtection(boolean protect)
	{
		if (Config.DEVELOPER && (protect || (_protectEndTime > 0)))
		{
			System.out.println(getName() + ": Protection " + (protect ? "ON " + (GameTimeController.getGameTicks() + (Config.PLAYER_SPAWN_PROTECTION * GameTimeController.TICKS_PER_SECOND)) : "OFF") + " (currently " + GameTimeController.getGameTicks() + ")");
		}
		
		_protectEndTime = protect ? GameTimeController.getGameTicks() + (Config.PLAYER_SPAWN_PROTECTION * GameTimeController.TICKS_PER_SECOND) : 0;
	}
	
	/**
	 * Set protection from agro mobs when getting up from fake death, according settings.
	 * @param protect the new recent fake death
	 */
	public void setRecentFakeDeath(boolean protect)
	{
		_recentFakeDeathEndTime = protect ? GameTimeController.getGameTicks() + (Config.PLAYER_FAKEDEATH_UP_PROTECTION * GameTimeController.TICKS_PER_SECOND) : 0;
	}
	
	/**
	 * Checks if is recent fake death.
	 * @return true, if is recent fake death
	 */
	public boolean isRecentFakeDeath()
	{
		return _recentFakeDeathEndTime > GameTimeController.getGameTicks();
	}
	
	/**
	 * Get the client owner of this char.
	 * @return the client
	 */
	public L2GameClient getClient()
	{
		return _client;
	}
	
	/**
	 * Sets the client.
	 * @param client the new client
	 */
	public void setClient(L2GameClient client)
	{
		_client = client;
	}
	
	/**
	 * Close the active connection with the client.
	 */
	public void closeNetConnection()
	{
		if (_client != null)
		{
			_client.close(new LeaveWorld());
		}
	}
	
	/**
	 * Manage actions when a player click on this L2PcInstance. <BR>
	 * <B><U> Actions on first click on the L2PcInstance (Select it)</U> :</B><BR>
	 * <li>Set the target of the player</li> <li>Send a Server->Client packet MyTargetSelected to the player (display the select window)</li><BR>
	 * <BR>
	 * <B><U> Actions on second click on the L2PcInstance (Follow it/Attack it/Intercat with it)</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client packet MyTargetSelected to the player (display the select window)</li> <li>If this L2PcInstance has a Private Store, notify the player AI with AI_INTENTION_INTERACT</li> <li>If this L2PcInstance is autoAttackable, notify the player AI with AI_INTENTION_ATTACK</li><BR>
	 * <BR>
	 * <li>If this L2PcInstance is NOT autoAttackable, notify the player AI with AI_INTENTION_FOLLOW</li><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Client packet : Action, AttackRequest</li><BR>
	 * @param player The player that start an action on this L2PcInstance
	 */
	@Override
	public void onAction(L2PcInstance player)
	{
		// See description in TvTEvent.java
		if (!TvTEvent.onAction(player.getName(), getName()))
		{
			player.sendPacket(new ActionFailed());
			return;
		}
		
		// Check if the L2PcInstance is confused
		if (player.isOutOfControl())
		{
			// Send a Server->Client packet ActionFailed to the player
			player.sendPacket(new ActionFailed());
			return;
		}
		
		// Check if the player already target this L2PcInstance
		if (player.getTarget() != this)
		{
			// Set the target of the player
			player.setTarget(this);
			
			// Send a Server->Client packet MyTargetSelected to the player
			// The color to display in the select window is White
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
			if (player != this)
			{
				player.sendPacket(new ValidateLocation(this));
			}
		}
		else
		{
			if (player != this)
			{
				player.sendPacket(new ValidateLocation(this));
			}
			// Check if this L2PcInstance has a Private Store
			if (getPrivateStoreType() != 0)
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
			else
			{
				// Check if this L2PcInstance is autoAttackable
				if (isAutoAttackable(player))
				{
					// Player with lvl < 21 can't attack a cursed weapon holder
					// And a cursed weapon holder can't attack players with lvl < 21
					if ((isCursedWeaponEquiped() && (player.getLevel() < 21)) || (player.isCursedWeaponEquiped() && (getLevel() < 21)))
					{
						player.sendPacket(new ActionFailed());
					}
					else
					{
						if (Config.GEODATA > 0)
						{
							if (GeoData.getInstance().canSeeTarget(player, this))
							{
								player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
								player.onActionRequest();
							}
						}
						else
						{
							player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
							player.onActionRequest();
						}
					}
				}
				else
				{
					if (Config.GEODATA > 0)
					{
						if (GeoData.getInstance().canSeeTarget(player, this))
						{
							player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
						}
					}
					else
					{
						player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this);
					}
				}
			}
		}
	}
	
	/**
	 * Returns true if cp update should be done, false if not.
	 * @param barPixels the bar pixels
	 * @return boolean
	 */
	private boolean needCpUpdate(int barPixels)
	{
		double currentCp = getCurrentCp();
		
		if ((currentCp <= 1.0) || (getMaxCp() < barPixels))
		{
			return true;
		}
		
		if ((currentCp <= _cpUpdateDecCheck) || (currentCp >= _cpUpdateIncCheck))
		{
			if (currentCp == getMaxCp())
			{
				_cpUpdateIncCheck = currentCp + 1;
				_cpUpdateDecCheck = currentCp - _cpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentCp / _cpUpdateInterval;
				int intMulti = (int) doubleMulti;
				
				_cpUpdateDecCheck = _cpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_cpUpdateIncCheck = _cpUpdateDecCheck + _cpUpdateInterval;
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Returns true if mp update should be done, false if not.
	 * @param barPixels the bar pixels
	 * @return boolean
	 */
	private boolean needMpUpdate(int barPixels)
	{
		double currentMp = getCurrentMp();
		
		if ((currentMp <= 1.0) || (getMaxMp() < barPixels))
		{
			return true;
		}
		
		if ((currentMp <= _mpUpdateDecCheck) || (currentMp >= _mpUpdateIncCheck))
		{
			if (currentMp == getMaxMp())
			{
				_mpUpdateIncCheck = currentMp + 1;
				_mpUpdateDecCheck = currentMp - _mpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentMp / _mpUpdateInterval;
				int intMulti = (int) doubleMulti;
				
				_mpUpdateDecCheck = _mpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_mpUpdateIncCheck = _mpUpdateDecCheck + _mpUpdateInterval;
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Send packet StatusUpdate with current HP,MP and CP to the L2PcInstance and only current HP, MP and Level to all other L2PcInstance of the Party.<BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send the Server->Client packet StatusUpdate with current HP, MP and CP to this L2PcInstance</li><BR>
	 * <li>Send the Server->Client packet PartySmallWindowUpdate with current HP, MP and Level to all other L2PcInstance of the Party</li><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND current HP and MP to all L2PcInstance of the _statusListener</B></FONT><BR>
	 */
	@Override
	public void broadcastStatusUpdate()
	{
		// TODO We mustn't send these informations to other players
		// Send the Server->Client packet StatusUpdate with current HP and MP to all L2PcInstance that must be informed of HP/MP updates of this L2PcInstance
		// super.broadcastStatusUpdate();
		
		// Send the Server->Client packet StatusUpdate with current HP, MP and CP to this L2PcInstance
		StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
		su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
		su.addAttribute(StatusUpdate.CUR_CP, (int) getCurrentCp());
		su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
		sendPacket(su);
		
		// Check if a party is in progress and party window update is usefull
		if (isInParty() && (needCpUpdate(352) || super.needHpUpdate(352) || needMpUpdate(352)))
		{
			if (Config.DEBUG)
			{
				_log.fine("Send status for party window of " + getObjectId() + "(" + getName() + ") to his party. CP: " + getCurrentCp() + " HP: " + getCurrentHp() + " MP: " + getCurrentMp());
			}
			// Send the Server->Client packet PartySmallWindowUpdate with current HP, MP and Level to all other L2PcInstance of the Party
			PartySmallWindowUpdate update = new PartySmallWindowUpdate(this);
			getParty().broadcastToPartyMembers(this, update);
		}
		
		if (isInOlympiadMode())
		{
			// TODO: implement new OlympiadUserInfo
			for (L2PcInstance player : getKnownList().getKnownPlayers().values())
			{
				if (player.getOlympiadGameId() == getOlympiadGameId())
				{
					if (Config.DEBUG)
					{
						_log.fine("Send status for Olympia window of " + getObjectId() + "(" + getName() + ") to " + player.getObjectId() + "(" + player.getName() + "). CP: " + getCurrentCp() + " HP: " + getCurrentHp() + " MP: " + getCurrentMp());
					}
					player.sendPacket(new ExOlympiadUserInfo(this));
				}
			}
			if (Olympiad.getInstance().getSpectators(_olympiadGameId) != null)
			{
				for (L2PcInstance spectator : Olympiad.getInstance().getSpectators(_olympiadGameId))
				{
					if (spectator == null)
					{
						continue;
					}
					spectator.sendPacket(new ExOlympiadUserInfoSpectator(this, getOlympiadSide()));
				}
			}
		}
		if (isInDuel())
		{
			ExDuelUpdateUserInfo update = new ExDuelUpdateUserInfo(this);
			DuelManager.getInstance().broadcastToOppositTeam(this, update);
		}
	}
	
	/**
	 * Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers.<BR>
	 * <B><U> Concept</U> :</B><BR>
	 * Others L2PcInstance in the detection area of the L2PcInstance are identified in <B>_knownPlayers</B>. In order to inform other players of this L2PcInstance state modifications, server just need to go through _knownPlayers to send Server->Client Packet<BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>Send a Server->Client packet UserInfo to this L2PcInstance (Public and Private Data)</li> <li>Send a Server->Client packet CharInfo to all L2PcInstance in _KnownPlayers of the L2PcInstance (Public data only)</li><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : DON'T SEND UserInfo packet to other players instead of CharInfo packet. Indeed, UserInfo packet contains PRIVATE DATA as MaxHP, STR, DEX...</B></FONT><BR>
	 */
	public final void broadcastUserInfo()
	{
		// Send a Server->Client packet UserInfo to this L2PcInstance
		sendPacket(new UserInfo(this));
		
		// Send a Server->Client packet CharInfo to all L2PcInstance in _KnownPlayers of the L2PcInstance
		if (Config.DEBUG)
		{
			_log.fine("players to notify:" + getKnownList().getKnownPlayers().size() + " packet: [S] 03 CharInfo");
		}
		
		Broadcast.toKnownPlayers(this, new CharInfo(this));
	}
	
	/**
	 * Broadcast title info.
	 */
	public final void broadcastTitleInfo()
	{
		// Send a Server->Client packet UserInfo to this L2PcInstance
		sendPacket(new UserInfo(this));
		
		// Send a Server->Client packet TitleUpdate to all L2PcInstance in _KnownPlayers of the L2PcInstance
		if (Config.DEBUG)
		{
			_log.fine("players to notify:" + getKnownList().getKnownPlayers().size() + " packet: [S] cc TitleUpdate");
		}
		
		Broadcast.toKnownPlayers(this, new TitleUpdate(this));
	}
	
	/**
	 * Return the Alliance Identifier of the L2PcInstance.
	 * @return the ally id
	 */
	public int getAllyId()
	{
		if (_clan == null)
		{
			return 0;
		}
		return _clan.getAllyId();
	}
	
	/**
	 * Gets the ally crest id.
	 * @return the ally crest id
	 */
	public int getAllyCrestId()
	{
		if (getClanId() == 0)
		{
			return 0;
		}
		if (getClan().getAllyId() == 0)
		{
			return 0;
		}
		return getClan().getAllyCrestId();
	}
	
	/**
	 * Manage hit process (called by Hit Task of L2Character).<BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL and send a Server->Client packet ActionFailed (if attacker is a L2PcInstance)</li> <li>If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are L2PcInstance</li>
	 * <li>If attack isn't aborted and hit isn't missed, reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary</li> <li>if attack isn't aborted and hit isn't missed, manage attack or cast break of the target (calculating rate, sending message...)</li><BR>
	 * @param target The L2Character targeted
	 * @param damage Nb of HP to reduce
	 * @param crit True if hit is critical
	 * @param miss True if hit is missed
	 * @param soulshot True if SoulShot are charged
	 * @param shld True if shield is efficient
	 */
	@Override
	protected void onHitTimer(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, boolean shld)
	{
		super.onHitTimer(target, damage, crit, miss, soulshot, shld);
	}
	
	/**
	 * Send a Server->Client packet StatusUpdate to the L2PcInstance.
	 * @param packet the packet
	 */
	@Override
	public void sendPacket(L2GameServerPacket packet)
	{
		if (_client != null)
		{
			_client.sendPacket(packet);
		}
		/*
		 * if(_isConnected) { try { if (_connection != null) _connection.sendPacket(packet); } catch (Exception e) { _log.log(Level.INFO, "", e); } }
		 */
	}
	
	/**
	 * Manage Interact Task with another L2PcInstance.<BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>If the private store is a STORE_PRIVATE_SELL, send a Server->Client PrivateBuyListSell packet to the L2PcInstance</li> <li>If the private store is a STORE_PRIVATE_BUY, send a Server->Client PrivateBuyListBuy packet to the L2PcInstance</li> <li>If the private store is a
	 * STORE_PRIVATE_MANUFACTURE, send a Server->Client RecipeShopSellList packet to the L2PcInstance</li><BR>
	 * @param target The L2Character targeted
	 */
	public void doInteract(L2Character target)
	{
		if (target instanceof L2PcInstance)
		{
			L2PcInstance temp = (L2PcInstance) target;
			sendPacket(new ActionFailed());
			
			if ((temp.getPrivateStoreType() == STORE_PRIVATE_SELL) || (temp.getPrivateStoreType() == STORE_PRIVATE_PACKAGE_SELL))
			{
				sendPacket(new PrivateStoreListSell(this, temp));
			}
			else if (temp.getPrivateStoreType() == STORE_PRIVATE_BUY)
			{
				sendPacket(new PrivateStoreListBuy(this, temp));
			}
			else if (temp.getPrivateStoreType() == STORE_PRIVATE_MANUFACTURE)
			{
				sendPacket(new RecipeShopSellList(this, temp));
			}
			
		}
		else
		{
			// _interactTarget=null should never happen but one never knows ^^;
			if (target != null)
			{
				target.onAction(this);
			}
		}
	}
	
	/**
	 * Manage AutoLoot Task.<BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>Send a System Message to the L2PcInstance : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li> <li>Add the Item to the L2PcInstance inventory</li> <li>Send a Server->Client packet InventoryUpdate to this L2PcInstance with NewItem (use a new slot) or ModifiedItem (increase amount)</li> <li>
	 * Send a Server->Client packet StatusUpdate to this L2PcInstance with current weight</li><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress, distribute Items between party members</B></FONT><BR>
	 * @param target The L2ItemInstance dropped
	 * @param item the item
	 */
	public void doAutoLoot(L2Attackable target, L2Attackable.RewardItem item)
	{
		if (isInParty())
		{
			getParty().distributeItem(this, item, false, target);
		}
		else if (item.getItemId() == 57)
		{
			addAdena("Loot", item.getCount(), target, true);
		}
		else
		{
			addItem("Loot", item.getItemId(), item.getCount(), target, true);
		}
	}
	
	/**
	 * Manage Pickup Task.<BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>Send a Server->Client packet StopMove to this L2PcInstance</li> <li>Remove the L2ItemInstance from the world and send server->client GetItem packets</li> <li>Send a System Message to the L2PcInstance : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li> <li>Add the Item to the L2PcInstance
	 * inventory</li> <li>Send a Server->Client packet InventoryUpdate to this L2PcInstance with NewItem (use a new slot) or ModifiedItem (increase amount)</li> <li>Send a Server->Client packet StatusUpdate to this L2PcInstance with current weight</li><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress, distribute Items between party members</B></FONT><BR>
	 * @param object The L2ItemInstance to pick up
	 */
	protected void doPickupItem(L2Object object)
	{
		if (isAlikeDead() || isFakeDeath())
		{
			return;
		}
		
		// Set the AI Intention to AI_INTENTION_IDLE
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		
		// Check if the L2Object to pick up is a L2ItemInstance
		if (!(object instanceof L2ItemInstance))
		{
			// dont try to pickup anything that is not an item :)
			_log.warning("trying to pickup wrong target." + getTarget());
			return;
		}
		
		L2ItemInstance target = (L2ItemInstance) object;
		
		// Send a Server->Client packet ActionFailed to this L2PcInstance
		sendPacket(new ActionFailed());
		
		// Send a Server->Client packet StopMove to this L2PcInstance
		StopMove sm = new StopMove(getObjectId(), getX(), getY(), getZ(), getHeading());
		if (Config.DEBUG)
		{
			_log.fine("pickup pos: " + target.getX() + " " + target.getY() + " " + target.getZ());
		}
		sendPacket(sm);
		
		synchronized (target)
		{
			// Check if the target to pick up is visible
			if (!target.isVisible())
			{
				// Send a Server->Client packet ActionFailed to this L2PcInstance
				sendPacket(new ActionFailed());
				return;
			}
			
			if (((isInParty() && (getParty().getLootDistribution() == L2Party.ITEM_LOOTER)) || !isInParty()) && !_inventory.validateCapacity(target))
			{
				sendPacket(new ActionFailed());
				sendPacket(new SystemMessage(SystemMessageId.SLOTS_FULL));
				return;
			}
			
			if ((target.getOwnerId() != 0) && (target.getOwnerId() != getObjectId()) && !isInLooterParty(target.getOwnerId()))
			{
				sendPacket(new ActionFailed());
				
				if (target.getItemId() == 57)
				{
					SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA);
					smsg.addNumber(target.getCount());
					sendPacket(smsg);
				}
				else if (target.getCount() > 1)
				{
					SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S);
					smsg.addItemName(target.getItemId());
					smsg.addNumber(target.getCount());
					sendPacket(smsg);
				}
				else
				{
					SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
					smsg.addItemName(target.getItemId());
					sendPacket(smsg);
				}
				
				return;
			}
			if ((target.getItemLootShedule() != null) && ((target.getOwnerId() == getObjectId()) || isInLooterParty(target.getOwnerId())))
			{
				target.resetOwnerTimer();
			}
			
			// Remove the L2ItemInstance from the world and send server->client GetItem packets
			target.pickupMe(this);
			if (Config.SAVE_DROPPED_ITEM)
			{
				ItemsOnGroundManager.getInstance().removeObject(target);
			}
			
		}
		
		// Auto use herbs - pick up
		if (target.getItemType() == L2EtcItemType.HERB)
		{
			IItemHandler handler = ItemHandler.getInstance().getItemHandler(target.getItemId());
			if (handler == null)
			{
				_log.fine("No item handler registered for item ID " + target.getItemId() + ".");
			}
			else
			{
				handler.useItem(this, target);
			}
			ItemTable.getInstance().destroyItem("Consume", target, this, null);
		}
		// Cursed Weapons are not distributed
		else if (CursedWeaponsManager.getInstance().isCursed(target.getItemId()))
		{
			addItem("Pickup", target, null, true);
		}
		else
		{
			// if item is instance of L2ArmorType or L2WeaponType broadcast an "Attention" system message
			if ((target.getItemType() instanceof L2ArmorType) || (target.getItemType() instanceof L2WeaponType))
			{
				if (target.getEnchantLevel() > 0)
				{
					SystemMessage msg = new SystemMessage(SystemMessageId.ATTENTION_S1_PICKED_UP_S2_S3);
					msg.addString(getName());
					msg.addNumber(target.getEnchantLevel());
					msg.addItemName(target.getItemId());
					broadcastPacket(msg, 1400);
				}
				else
				{
					SystemMessage msg = new SystemMessage(SystemMessageId.ATTENTION_S1_PICKED_UP_S2);
					msg.addString(getName());
					msg.addItemName(target.getItemId());
					broadcastPacket(msg, 1400);
				}
			}
			
			// Check if a Party is in progress
			if (isInParty())
			{
				getParty().distributeItem(this, target);
			}
			else if ((target.getItemId() == 57) && (getInventory().getAdenaInstance() != null))
			{
				addAdena("Pickup", target.getCount(), null, true);
				ItemTable.getInstance().destroyItem("Pickup", target, this, null);
			}
			else
			{
				addItem("Pickup", target, null, true);
			}
		}
	}
	
	/**
	 * Set a target.<BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>Remove the L2PcInstance from the _statusListener of the old target if it was a L2Character</li> <li>Add the L2PcInstance to the _statusListener of the new target if it's a L2Character</li> <li>Target the new L2Object (add the target to the L2PcInstance _target, _knownObject and
	 * L2PcInstance to _KnownObject of the L2Object)</li><BR>
	 * @param newTarget The L2Object to target
	 */
	@Override
	public void setTarget(L2Object newTarget)
	{
		// Check if the new target is visible
		if ((newTarget != null) && !newTarget.isVisible())
		{
			newTarget = null;
		}
		
		// Prevents /target exploiting
		if ((newTarget != null) && (Math.abs(newTarget.getZ() - getZ()) > 1000))
		{
			newTarget = null;
		}
		
		if (!isGM())
		{
			// Can't target and attack festival monsters if not participant
			if ((newTarget instanceof L2FestivalMonsterInstance) && !isFestivalParticipant())
			{
				newTarget = null;
			}
			else if (isInParty() && getParty().isInDimensionalRift())
			{
				byte riftType = getParty().getDimensionalRift().getType();
				byte riftRoom = getParty().getDimensionalRift().getCurrentRoom();
				
				if ((newTarget != null) && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(newTarget.getX(), newTarget.getY(), newTarget.getZ()))
				{
					newTarget = null;
				}
			}
		}
		
		// Get the current target
		L2Object oldTarget = getTarget();
		
		if (oldTarget != null)
		{
			if (oldTarget.equals(newTarget))
			{
				return; // no target change
			}
			
			// Remove the L2PcInstance from the _statusListener of the old target if it was a L2Character
			if (oldTarget instanceof L2Character)
			{
				((L2Character) oldTarget).removeStatusListener(this);
			}
		}
		
		// Add the L2PcInstance to the _statusListener of the new target if it's a L2Character
		if ((newTarget != null) && (newTarget instanceof L2Character))
		{
			((L2Character) newTarget).addStatusListener(this);
			TargetSelected my = new TargetSelected(getObjectId(), newTarget.getObjectId(), getX(), getY(), getZ());
			broadcastPacket(my);
		}
		
		// Target the new L2Object (add the target to the L2PcInstance _target, _knownObject and L2PcInstance to _KnownObject of the L2Object)
		super.setTarget(newTarget);
	}
	
	/**
	 * Return the active weapon instance (always equiped in the right hand).
	 * @return the active weapon instance
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
	}
	
	/**
	 * Return the active weapon item (always equiped in the right hand).
	 * @return the active weapon item
	 */
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		L2ItemInstance weapon = getActiveWeaponInstance();
		
		if (weapon == null)
		{
			return getFistsWeaponItem();
		}
		
		return (L2Weapon) weapon.getItem();
	}
	
	/**
	 * Gets the chest armor instance.
	 * @return the chest armor instance
	 */
	public L2ItemInstance getChestArmorInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
	}
	
	/**
	 * Gets the active chest armor item.
	 * @return the active chest armor item
	 */
	public L2Armor getActiveChestArmorItem()
	{
		L2ItemInstance armor = getChestArmorInstance();
		
		if (armor == null)
		{
			return null;
		}
		
		return (L2Armor) armor.getItem();
	}
	
	/**
	 * Checks if is wearing heavy armor.
	 * @return true, if is wearing heavy armor
	 */
	public boolean isWearingHeavyArmor()
	{
		L2ItemInstance armor = getChestArmorInstance();
		
		if ((L2ArmorType) armor.getItemType() == L2ArmorType.HEAVY)
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Checks if is wearing light armor.
	 * @return true, if is wearing light armor
	 */
	public boolean isWearingLightArmor()
	{
		L2ItemInstance armor = getChestArmorInstance();
		
		if ((L2ArmorType) armor.getItemType() == L2ArmorType.LIGHT)
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Checks if is wearing magic armor.
	 * @return true, if is wearing magic armor
	 */
	public boolean isWearingMagicArmor()
	{
		L2ItemInstance armor = getChestArmorInstance();
		
		if ((L2ArmorType) armor.getItemType() == L2ArmorType.MAGIC)
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Checks if is wearing formal wear.
	 * @return true, if is wearing formal wear
	 */
	public boolean isWearingFormalWear()
	{
		return _IsWearingFormalWear;
	}
	
	/**
	 * Sets the checks if is wearing formal wear.
	 * @param value the new checks if is wearing formal wear
	 */
	public void setIsWearingFormalWear(boolean value)
	{
		_IsWearingFormalWear = value;
	}
	
	/**
	 * Checks if is married.
	 * @return true, if is married
	 */
	public boolean isMarried()
	{
		return _married;
	}
	
	/**
	 * Sets the married.
	 * @param state the new married
	 */
	public void setMarried(boolean state)
	{
		_married = state;
	}
	
	/**
	 * Checks if is engage request.
	 * @return true, if is engage request
	 */
	public boolean isEngageRequest()
	{
		return _engagerequest;
	}
	
	/**
	 * Sets the engage request.
	 * @param state the state
	 * @param playerid the playerid
	 */
	public void setEngageRequest(boolean state, int playerid)
	{
		_engagerequest = state;
		_engageid = playerid;
	}
	
	/**
	 * Sets the mary request.
	 * @param state the new mary request
	 */
	public void setMaryRequest(boolean state)
	{
		_marryrequest = state;
	}
	
	/**
	 * Checks if is mary request.
	 * @return true, if is mary request
	 */
	public boolean isMaryRequest()
	{
		return _marryrequest;
	}
	
	/**
	 * Sets the marry accepted.
	 * @param state the new marry accepted
	 */
	public void setMarryAccepted(boolean state)
	{
		_marryaccepted = state;
	}
	
	/**
	 * Checks if is marry accepted.
	 * @return true, if is marry accepted
	 */
	public boolean isMarryAccepted()
	{
		return _marryaccepted;
	}
	
	/**
	 * Gets the engage id.
	 * @return the engage id
	 */
	public int getEngageId()
	{
		return _engageid;
	}
	
	/**
	 * Gets the partner id.
	 * @return the partner id
	 */
	public int getPartnerId()
	{
		return _partnerId;
	}
	
	/**
	 * Sets the partner id.
	 * @param partnerid the new partner id
	 */
	public void setPartnerId(int partnerid)
	{
		_partnerId = partnerid;
	}
	
	/**
	 * Gets the couple id.
	 * @return the couple id
	 */
	public int getCoupleId()
	{
		return _coupleId;
	}
	
	/**
	 * Sets the couple id.
	 * @param coupleId the new couple id
	 */
	public void setCoupleId(int coupleId)
	{
		_coupleId = coupleId;
	}
	
	/**
	 * Engage answer.
	 * @param answer the answer
	 */
	public void EngageAnswer(int answer)
	{
		if (_engagerequest == false)
		{
			return;
		}
		else if (_engageid == 0)
		{
			return;
		}
		else
		{
			L2PcInstance ptarget = (L2PcInstance) L2World.getInstance().findObject(_engageid);
			setEngageRequest(false, 0);
			if (ptarget != null)
			{
				if (answer == 1)
				{
					CoupleManager.getInstance().createCouple(ptarget, L2PcInstance.this);
					ptarget.sendMessage("Request to Engage has been >ACCEPTED<");
				}
				else
				{
					ptarget.sendMessage("Request to Engage has been >DENIED<!");
				}
			}
		}
	}
	
	/**
	 * Return the secondary weapon instance (always equiped in the left hand).
	 * @return the secondary weapon instance
	 */
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
	}
	
	/**
	 * Return the secondary weapon item (always equiped in the left hand) or the fists weapon.
	 * @return the secondary weapon item
	 */
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		L2ItemInstance weapon = getSecondaryWeaponInstance();
		
		if (weapon == null)
		{
			return getFistsWeaponItem();
		}
		
		L2Item item = weapon.getItem();
		
		if (item instanceof L2Weapon)
		{
			return (L2Weapon) item;
		}
		
		return null;
	}
	
	/**
	 * Kill the L2Character, Apply Death Penalty, Manage gain/loss Karma and Item Drop. <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>Reduce the Experience of the L2PcInstance in function of the calculated Death Penalty</li> <li>If necessary, unsummon the Pet of the killed L2PcInstance</li> <li>Manage Karma gain for attacker and Karam loss for the killed L2PcInstance</li> <li>If the killed L2PcInstance has Karma, manage
	 * Drop Item</li> <li>Kill the L2PcInstance</li><BR>
	 * @param killer the killer
	 * @return true, if successful
	 */
	@Override
	public boolean doDie(L2Character killer)
	{
		// Kill the L2PcInstance
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if (killer != null)
		{
			L2PcInstance pk = null;
			if (killer instanceof L2PcInstance)
			{
				pk = (L2PcInstance) killer;
			}
			
			TvTEvent.onKill(killer, this);
			
			if (atEvent && (pk != null))
			{
				pk.kills.add(getName());
			}
			
			// Clear resurrect xp calculation
			setExpBeforeDeath(0);
			
			if (isCursedWeaponEquiped())
			{
				CursedWeaponsManager.getInstance().drop(_cursedWeaponEquipedId, killer);
			}
			else
			{
				if ((pk == null) || !pk.isCursedWeaponEquiped())
				{
					// if (getKarma() > 0)
					onDieDropItem(killer); // Check if any item should be dropped
					
					if (!(isInsideZone(ZONE_PVP) && !isInsideZone(ZONE_SIEGE)))
					{
						boolean isKillerPc = (killer instanceof L2PcInstance);
						if (isKillerPc && (((L2PcInstance) killer).getClan() != null) && (getClan() != null) && !isAcademyMember() && !(((L2PcInstance) killer).isAcademyMember()) && _clan.isAtWarWith(((L2PcInstance) killer).getClanId()) && ((L2PcInstance) killer).getClan().isAtWarWith(_clan.getClanId()))
						{
							if (getClan().getReputationScore() > 0)
							{
								((L2PcInstance) killer).getClan().setReputationScore(((L2PcInstance) killer).getClan().getReputationScore() + 2, true);
							}
							if (((L2PcInstance) killer).getClan().getReputationScore() > 0)
							{
								_clan.setReputationScore(_clan.getReputationScore() - 2, true);
							}
						}
						if (Config.ALT_GAME_DELEVEL)
						{
							// Reduce the Experience of the L2PcInstance in function of the calculated Death Penalty
							// NOTE: deathPenalty +- Exp will update karma
							if ((getSkillLevel(L2Skill.SKILL_LUCKY) < 0) || (getStat().getLevel() > 9))
							{
								deathPenalty(((pk != null) && (getClan() != null) && (pk.getClan() != null) && pk.getClan().isAtWarWith(getClanId())));
							}
						}
						else
						{
							onDieUpdateKarma(); // Update karma if delevel is not allowed
						}
					}
				}
			}
		}
		
		setPvpFlag(0); // Clear the pvp flag
		
		// Unsummon Cubics
		if (_cubics.size() > 0)
		{
			for (L2CubicInstance cubic : _cubics.values())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}
			
			_cubics.clear();
		}
		
		if (_forceBuff != null)
		{
			_forceBuff.delete();
		}
		
		for (L2Character character : getKnownList().getKnownCharacters())
		{
			if ((character.getForceBuff() != null) && (character.getForceBuff().getTarget() == this))
			{
				character.abortCast();
			}
		}
		
		if (isInParty() && getParty().isInDimensionalRift())
		{
			getParty().getDimensionalRift().getDeadMemberList().add(this);
		}
		
		// calculate death penalty buff
		calculateDeathPenaltyBuffLevel(killer);
		
		stopRentPet();
		stopWaterTask();
		return true;
	}
	
	/**
	 * On die drop item.
	 * @param killer the killer
	 */
	private void onDieDropItem(L2Character killer)
	{
		if (atEvent || (killer == null))
		{
			return;
		}
		
		if ((getKarma() <= 0) && (killer instanceof L2PcInstance) && (((L2PcInstance) killer).getClan() != null) && (getClan() != null) && (((L2PcInstance) killer).getClan().isAtWarWith(getClanId())
		// || this.getClan().isAtWarWith(((L2PcInstance)killer).getClanId())
		))
		{
			return;
		}
		
		if (!isInsideZone(ZONE_PVP) && (!isGM() || Config.KARMA_DROP_GM))
		{
			boolean isKarmaDrop = false;
			boolean isKillerNpc = (killer instanceof L2NpcInstance);
			int pkLimit = Config.KARMA_PK_LIMIT;
			
			int dropEquip = 0;
			int dropEquipWeapon = 0;
			int dropItem = 0;
			int dropLimit = 0;
			int dropPercent = 0;
			
			if ((getKarma() > 0) && (getPkKills() >= pkLimit))
			{
				isKarmaDrop = true;
				dropPercent = Config.KARMA_RATE_DROP;
				dropEquip = Config.KARMA_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.KARMA_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.KARMA_RATE_DROP_ITEM;
				dropLimit = Config.KARMA_DROP_LIMIT;
			}
			else if (isKillerNpc && (getLevel() > 4) && !isFestivalParticipant())
			{
				dropPercent = Config.PLAYER_RATE_DROP;
				dropEquip = Config.PLAYER_RATE_DROP_EQUIP;
				dropEquipWeapon = Config.PLAYER_RATE_DROP_EQUIP_WEAPON;
				dropItem = Config.PLAYER_RATE_DROP_ITEM;
				dropLimit = Config.PLAYER_DROP_LIMIT;
			}
			
			int dropCount = 0;
			while ((dropPercent > 0) && (Rnd.get(100) < dropPercent) && (dropCount < dropLimit))
			{
				int itemDropPercent = 0;
				List<Integer> nonDroppableList = new FastList<>();
				List<Integer> nonDroppableListPet = new FastList<>();
				
				nonDroppableList = Config.KARMA_LIST_NONDROPPABLE_ITEMS;
				nonDroppableListPet = Config.KARMA_LIST_NONDROPPABLE_ITEMS;
				
				for (L2ItemInstance itemDrop : getInventory().getItems())
				{
					// Don't drop
					if (itemDrop.isAugmented() || // Dont drop augmented items
					itemDrop.isShadowItem() || // Dont drop Shadow Items
					(itemDrop.getItemId() == 57) || // Adena
					(itemDrop.getItem().getType2() == L2Item.TYPE2_QUEST) || // Quest Items
					nonDroppableList.contains(itemDrop.getItemId()) || // Item listed in the non droppable item list
					nonDroppableListPet.contains(itemDrop.getItemId()) || // Item listed in the non droppable pet item list
					((getPet() != null) && (getPet().getControlItemId() == itemDrop.getItemId() // Control Item of active pet
					)))
					{
						continue;
					}
					
					if (itemDrop.isEquipped())
					{
						// Set proper chance according to Item type of equipped Item
						itemDropPercent = itemDrop.getItem().getType2() == L2Item.TYPE2_WEAPON ? dropEquipWeapon : dropEquip;
						getInventory().unEquipItemInSlotAndRecord(itemDrop.getEquipSlot());
					}
					else
					{
						itemDropPercent = dropItem; // Item in inventory
					}
					
					// NOTE: Each time an item is dropped, the chance of another item being dropped gets lesser (dropCount * 2)
					if (Rnd.get(100) < itemDropPercent)
					{
						dropItem("DieDrop", itemDrop, killer, true);
						
						if (isKarmaDrop)
						{
							_log.warning(getName() + " has karma and dropped id = " + itemDrop.getItemId() + ", count = " + itemDrop.getCount());
						}
						else
						{
							_log.warning(getName() + " dropped id = " + itemDrop.getItemId() + ", count = " + itemDrop.getCount());
						}
						
						dropCount++;
						break;
					}
				}
			}
		}
	}
	
	/**
	 * On die update karma.
	 */
	private void onDieUpdateKarma()
	{
		// Karma lose for server that does not allow delevel
		if (getKarma() > 0)
		{
			// this formula seems to work relatively well:
			// baseKarma * thisLVL * (thisLVL/100)
			// Calculate the new Karma of the attacker : newKarma = baseKarma*pkCountMulti*lvlDiffMulti
			double karmaLost = Config.KARMA_LOST_BASE;
			karmaLost *= getLevel(); // multiply by char lvl
			karmaLost *= (getLevel() / 100.0); // divide by 0.charLVL
			karmaLost = Math.round(karmaLost);
			if (karmaLost < 0)
			{
				karmaLost = 1;
			}
			
			// Decrease Karma of the L2PcInstance and Send it a Server->Client StatusUpdate packet with Karma and PvP Flag if necessary
			setKarma(getKarma() - (int) karmaLost);
		}
	}
	
	/**
	 * On kill update pvp karma.
	 * @param target the target
	 */
	public void onKillUpdatePvPKarma(L2Character target)
	{
		if (target == null)
		{
			return;
		}
		if (!(target instanceof L2PlayableInstance))
		{
			return;
		}
		
		L2PcInstance targetPlayer = null;
		if (target instanceof L2PcInstance)
		{
			targetPlayer = (L2PcInstance) target;
		}
		else if (target instanceof L2Summon)
		{
			targetPlayer = ((L2Summon) target).getOwner();
		}
		
		if (targetPlayer == null)
		{
			return; // Target player is null
		}
		if (targetPlayer == this)
		{
			return; // Target player is self
		}
		
		if (isCursedWeaponEquiped())
		{
			CursedWeaponsManager.getInstance().increaseKills(_cursedWeaponEquipedId);
			return;
		}
		
		// If in duel and you kill (only can kill l2summon), do nothing
		if (isInDuel() && targetPlayer.isInDuel())
		{
			return;
		}
		
		// If in Arena, do nothing
		if (isInsideZone(ZONE_PVP) || targetPlayer.isInsideZone(ZONE_PVP))
		{
			return;
		}
		
		// Check if it's pvp
		if ((checkIfPvP(target) && // Can pvp and
		(targetPlayer.getPvpFlag() != 0 // Target player has pvp flag set
		)) || // or
		(isInsideZone(ZONE_PVP) && // Player is inside pvp zone and
		targetPlayer.isInsideZone(ZONE_PVP) // Target player is inside pvp zone
		))
		{
			increasePvpKills();
		}
		else
		// Target player doesn't have pvp flag set
		{
			// check about wars
			if ((targetPlayer.getClan() != null) && (getClan() != null))
			{
				if (getClan().isAtWarWith(targetPlayer.getClanId()))
				{
					if (targetPlayer.getClan().isAtWarWith(getClanId()))
					{
						// 'Both way war' -> 'PvP Kill'
						increasePvpKills();
						return;
					}
				}
			}
			
			// 'No war' or 'One way war' -> 'Normal PK'
			if (targetPlayer.getKarma() > 0) // Target player has karma
			{
				if (Config.KARMA_AWARD_PK_KILL)
				{
					increasePvpKills();
				}
			}
			else if (targetPlayer.getPvpFlag() == 0) // Target player doesn't have karma
			{
				increasePkKillsAndKarma(targetPlayer.getLevel());
			}
		}
	}
	
	/**
	 * Increase the pvp kills count and send the info to the player.
	 */
	public void increasePvpKills()
	{
		// Add karma to attacker and increase its PK counter
		setPvpKills(getPvpKills() + 1);
		
		// Send a Server->Client UserInfo packet to attacker with its Karma and PK Counter
		sendPacket(new UserInfo(this));
	}
	
	/**
	 * Increase pk count, karma and send the info to the player.
	 * @param targLVL : level of the killed player
	 */
	public void increasePkKillsAndKarma(int targLVL)
	{
		int baseKarma = Config.KARMA_MIN_KARMA;
		int newKarma = baseKarma;
		int karmaLimit = Config.KARMA_MAX_KARMA;
		
		int pkLVL = getLevel();
		int pkPKCount = getPkKills();
		
		int lvlDiffMulti = 0;
		int pkCountMulti = 0;
		
		// Check if the attacker has a PK counter greater than 0
		if (pkPKCount > 0)
		{
			pkCountMulti = pkPKCount / 2;
		}
		else
		{
			pkCountMulti = 1;
		}
		if (pkCountMulti < 1)
		{
			pkCountMulti = 1;
		}
		
		// Calculate the level difference Multiplier between attacker and killed L2PcInstance
		if (pkLVL > targLVL)
		{
			lvlDiffMulti = pkLVL / targLVL;
		}
		else
		{
			lvlDiffMulti = 1;
		}
		if (lvlDiffMulti < 1)
		{
			lvlDiffMulti = 1;
		}
		
		// Calculate the new Karma of the attacker : newKarma = baseKarma*pkCountMulti*lvlDiffMulti
		newKarma *= pkCountMulti;
		newKarma *= lvlDiffMulti;
		
		// Make sure newKarma is less than karmaLimit and higher than baseKarma
		if (newKarma < baseKarma)
		{
			newKarma = baseKarma;
		}
		if (newKarma > karmaLimit)
		{
			newKarma = karmaLimit;
		}
		
		// Fix to prevent overflow (=> karma has a max value of 2 147 483 647)
		if (getKarma() > (Integer.MAX_VALUE - newKarma))
		{
			newKarma = Integer.MAX_VALUE - getKarma();
		}
		
		// Add karma to attacker and increase its PK counter
		setPkKills(getPkKills() + 1);
		setKarma(getKarma() + newKarma);
		
		// Send a Server->Client UserInfo packet to attacker with its Karma and PK Counter
		sendPacket(new UserInfo(this));
	}
	
	/**
	 * Calculate karma lost.
	 * @param exp the exp
	 * @return the int
	 */
	public int calculateKarmaLost(long exp)
	{
		// KARMA LOSS
		// When a PKer gets killed by another player or a L2MonsterInstance, it loses a certain amount of Karma based on their level.
		// this (with defaults) results in a level 1 losing about ~2 karma per death, and a lvl 70 loses about 11760 karma per death...
		// You lose karma as long as you were not in a pvp zone and you did not kill urself.
		// NOTE: exp for death (if delevel is allowed) is based on the players level
		
		long expGained = Math.abs(exp);
		expGained /= Config.KARMA_XP_DIVIDER;
		
		// FIXME Micht : Maybe this code should be fixed and karma set to a long value
		int karmaLost = 0;
		if (expGained > Integer.MAX_VALUE)
		{
			karmaLost = Integer.MAX_VALUE;
		}
		else
		{
			karmaLost = (int) expGained;
		}
		
		if (karmaLost < Config.KARMA_LOST_BASE)
		{
			karmaLost = Config.KARMA_LOST_BASE;
		}
		if (karmaLost > getKarma())
		{
			karmaLost = getKarma();
		}
		
		return karmaLost;
	}
	
	/**
	 * Update pvp status.
	 */
	public void updatePvPStatus()
	{
		if (isInsideZone(ZONE_PVP))
		{
			return;
		}
		setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
		
		if (getPvpFlag() == 0)
		{
			startPvPFlag();
		}
	}
	
	/**
	 * Update pvp status.
	 * @param target the target
	 */
	public void updatePvPStatus(L2Character target)
	{
		L2PcInstance player_target = null;
		
		if (target instanceof L2PcInstance)
		{
			player_target = (L2PcInstance) target;
		}
		else if (target instanceof L2Summon)
		{
			player_target = ((L2Summon) target).getOwner();
		}
		
		if (player_target == null)
		{
			return;
		}
		
		if ((isInDuel() && (player_target.getDuelId() == getDuelId())))
		{
			return;
		}
		if ((!isInsideZone(ZONE_PVP) || !player_target.isInsideZone(ZONE_PVP)) && (player_target.getKarma() == 0))
		{
			if (checkIfPvP(player_target))
			{
				setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_PVP_TIME);
			}
			else
			{
				setPvpFlagLasts(System.currentTimeMillis() + Config.PVP_NORMAL_TIME);
			}
			if (getPvpFlag() == 0)
			{
				startPvPFlag();
			}
		}
	}
	
	/**
	 * Restore the specified % of experience this L2PcInstance has lost and sends a Server->Client StatusUpdate packet.
	 * @param restorePercent the restore percent
	 */
	public void restoreExp(double restorePercent)
	{
		if (getExpBeforeDeath() > 0)
		{
			// Restore the specified % of lost experience.
			getStat().addExp((int) Math.round(((getExpBeforeDeath() - getExp()) * restorePercent) / 100));
			setExpBeforeDeath(0);
		}
	}
	
	/**
	 * Reduce the Experience (and level if necessary) of the L2PcInstance in function of the calculated Death Penalty.<BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>Calculate the Experience loss</li> <li>Set the value of _expBeforeDeath</li> <li>Set the new Experience value of the L2PcInstance and Decrease its level if necessary</li> <li>Send a Server->Client StatusUpdate packet with its new Experience</li><BR>
	 * @param atwar the atwar
	 */
	public void deathPenalty(boolean atwar)
	{
		// TODO Need Correct Penalty
		// Get the level of the L2PcInstance
		final int lvl = getLevel();
		
		// The death steal you some Exp
		double percentLost = 7.0;
		if (getLevel() >= 76)
		{
			percentLost = 2.0;
		}
		else if (getLevel() >= 40)
		{
			percentLost = 4.0;
		}
		
		if (getKarma() > 0)
		{
			percentLost *= Config.RATE_KARMA_EXP_LOST;
		}
		
		if (isFestivalParticipant() || atwar || isInsideZone(ZONE_SIEGE))
		{
			percentLost /= 4.0;
		}
		
		// Calculate the Experience loss
		long lostExp = 0;
		if (!atEvent)
		{
			if (lvl < Experience.MAX_LEVEL)
			{
				lostExp = Math.round(((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentLost) / 100);
			}
			else
			{
				lostExp = Math.round(((getStat().getExpForLevel(Experience.MAX_LEVEL) - getStat().getExpForLevel(Experience.MAX_LEVEL - 1)) * percentLost) / 100);
			}
		}
		
		// Get the Experience before applying penalty
		setExpBeforeDeath(getExp());
		
		if (getCharmOfCourage())
		{
			if ((getSiegeState() > 0) && isInsideZone(ZONE_SIEGE))
			{
				lostExp = 0;
			}
		}
		
		setCharmOfCourage(false);
		
		if (Config.DEBUG)
		{
			_log.fine(getName() + " died and lost " + lostExp + " experience.");
		}
		
		// Set the new Experience value of the L2PcInstance
		getStat().addExp(-lostExp);
	}
	
	/**
	 * Sets the party matching automatic registration.
	 * @param b the new party matching automatic registration
	 */
	public void setPartyMatchingAutomaticRegistration(boolean b)
	{
		_partyMatchingAutomaticRegistration = b;
	}
	
	/**
	 * Sets the party matching show level.
	 * @param b the new party matching show level
	 */
	public void setPartyMatchingShowLevel(boolean b)
	{
		_partyMatchingShowLevel = b;
	}
	
	/**
	 * Sets the party matching show class.
	 * @param b the new party matching show class
	 */
	public void setPartyMatchingShowClass(boolean b)
	{
		_partyMatchingShowClass = b;
	}
	
	/**
	 * Sets the party matching memo.
	 * @param memo the new party matching memo
	 */
	public void setPartyMatchingMemo(String memo)
	{
		_partyMatchingMemo = memo;
	}
	
	/**
	 * Checks if is party matching automatic registration.
	 * @return true, if is party matching automatic registration
	 */
	public boolean isPartyMatchingAutomaticRegistration()
	{
		return _partyMatchingAutomaticRegistration;
	}
	
	/**
	 * Gets the party matching memo.
	 * @return the party matching memo
	 */
	public String getPartyMatchingMemo()
	{
		return _partyMatchingMemo;
	}
	
	/**
	 * Checks if is party matching show class.
	 * @return true, if is party matching show class
	 */
	public boolean isPartyMatchingShowClass()
	{
		return _partyMatchingShowClass;
	}
	
	/**
	 * Checks if is party matching show level.
	 * @return true, if is party matching show level
	 */
	public boolean isPartyMatchingShowLevel()
	{
		return _partyMatchingShowLevel;
	}
	
	/**
	 * Manage the increase level task of a L2PcInstance (Max MP, Max MP, Recommandation, Expertise and beginner skills...).<BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>Send a Server->Client System Message to the L2PcInstance : YOU_INCREASED_YOUR_LEVEL</li> <li>Send a Server->Client packet StatusUpdate to the L2PcInstance with new LEVEL, MAX_HP and MAX_MP</li> <li>Set the current HP and MP of the L2PcInstance, Launch/Stop a HP/MP/CP Regeneration Task and
	 * send StatusUpdate packet to all other L2PcInstance to inform (exclusive broadcast)</li> <li>Recalculate the party level</li> <li>Recalculate the number of Recommandation that the L2PcInstance can give</li> <li>Give Expertise skill of this level and remove beginner Lucky skill</li><BR>
	 */
	public void increaseLevel()
	{
		// Set the current HP and MP of the L2Character, Launch/Stop a HP/MP/CP Regeneration Task and send StatusUpdate packet to all other L2PcInstance to inform (exclusive broadcast)
		setCurrentHpMp(getMaxHp(), getMaxMp());
		setCurrentCp(getMaxCp());
	}
	
	/**
	 * Stop the HP/MP/CP Regeneration task.<BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>Set the RegenActive flag to False</li> <li>Stop the HP/MP/CP Regeneration task</li><BR>
	 */
	public void stopAllTimers()
	{
		stopHpMpRegeneration();
		stopWarnUserTakeBreak();
		stopWaterTask();
		stopRentPet();
		stopPvpRegTask();
		stopJailTask(true);
	}
	
	/**
	 * Return the L2Summon of the L2PcInstance or null.
	 * @return the pet
	 */
	@Override
	public L2Summon getPet()
	{
		return _summon;
	}
	
	/**
	 * Set the L2Summon of the L2PcInstance.
	 * @param summon the new pet
	 */
	public void setPet(L2Summon summon)
	{
		_summon = summon;
	}
	
	/**
	 * Return the L2Summon of the L2PcInstance or null.
	 * @return the trained beast
	 */
	public L2TamedBeastInstance getTrainedBeast()
	{
		return _tamedBeast;
	}
	
	/**
	 * Set the L2Summon of the L2PcInstance.
	 * @param tamedBeast the new trained beast
	 */
	public void setTrainedBeast(L2TamedBeastInstance tamedBeast)
	{
		_tamedBeast = tamedBeast;
	}
	
	/**
	 * Return the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
	 * @return the request
	 */
	public L2Request getRequest()
	{
		return _request;
	}
	
	/**
	 * Set the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
	 * @param requester the new active requester
	 */
	public synchronized void setActiveRequester(L2PcInstance requester)
	{
		_activeRequester = requester;
	}
	
	/**
	 * Return the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
	 * @return the active requester
	 */
	public synchronized L2PcInstance getActiveRequester()
	{
		return _activeRequester;
	}
	
	/**
	 * Return True if a transaction is in progress.
	 * @return true, if is processing request
	 */
	public boolean isProcessingRequest()
	{
		return (_activeRequester != null) || (_requestExpireTime > GameTimeController.getGameTicks());
	}
	
	/**
	 * Return True if a transaction is in progress.
	 * @return true, if is processing transaction
	 */
	public boolean isProcessingTransaction()
	{
		return (_activeRequester != null) || (_activeTradeList != null) || (_requestExpireTime > GameTimeController.getGameTicks());
	}
	
	/**
	 * Select the Warehouse to be used in next activity.
	 * @param partner the partner
	 */
	public void onTransactionRequest(L2PcInstance partner)
	{
		_requestExpireTime = GameTimeController.getGameTicks() + (REQUEST_TIMEOUT * GameTimeController.TICKS_PER_SECOND);
		partner.setActiveRequester(this);
	}
	
	/**
	 * Select the Warehouse to be used in next activity.
	 */
	public void onTransactionResponse()
	{
		_requestExpireTime = 0;
	}
	
	/**
	 * Select the Warehouse to be used in next activity.
	 * @param warehouse the new active warehouse
	 */
	public void setActiveWarehouse(ItemContainer warehouse)
	{
		_activeWarehouse = warehouse;
	}
	
	/**
	 * Return active Warehouse.
	 * @return the active warehouse
	 */
	public ItemContainer getActiveWarehouse()
	{
		return _activeWarehouse;
	}
	
	/**
	 * Select the TradeList to be used in next activity.
	 * @param tradeList the new active trade list
	 */
	public void setActiveTradeList(TradeList tradeList)
	{
		_activeTradeList = tradeList;
	}
	
	/**
	 * Return active TradeList.
	 * @return the active trade list
	 */
	public TradeList getActiveTradeList()
	{
		return _activeTradeList;
	}
	
	/**
	 * On trade start.
	 * @param partner the partner
	 */
	public void onTradeStart(L2PcInstance partner)
	{
		_activeTradeList = new TradeList(this);
		_activeTradeList.setPartner(partner);
		
		SystemMessage msg = new SystemMessage(SystemMessageId.BEGIN_TRADE_WITH_S1);
		msg.addString(partner.getName());
		sendPacket(msg);
		sendPacket(new TradeStart(this));
	}
	
	/**
	 * On trade confirm.
	 * @param partner the partner
	 */
	public void onTradeConfirm(L2PcInstance partner)
	{
		SystemMessage msg = new SystemMessage(SystemMessageId.S1_CONFIRMED_TRADE);
		msg.addString(partner.getName());
		sendPacket(msg);
	}
	
	/**
	 * On trade cancel.
	 * @param partner the partner
	 */
	public void onTradeCancel(L2PcInstance partner)
	{
		if (_activeTradeList == null)
		{
			return;
		}
		
		_activeTradeList.lock();
		_activeTradeList = null;
		
		sendPacket(new SendTradeDone(0));
		SystemMessage msg = new SystemMessage(SystemMessageId.S1_CANCELED_TRADE);
		msg.addString(partner.getName());
		sendPacket(msg);
	}
	
	/**
	 * On trade finish.
	 * @param successfull the successfull
	 */
	public void onTradeFinish(boolean successfull)
	{
		_activeTradeList = null;
		sendPacket(new SendTradeDone(1));
		if (successfull)
		{
			sendPacket(new SystemMessage(SystemMessageId.TRADE_SUCCESSFUL));
		}
	}
	
	/**
	 * Start trade.
	 * @param partner the partner
	 */
	public void startTrade(L2PcInstance partner)
	{
		onTradeStart(partner);
		partner.onTradeStart(this);
	}
	
	/**
	 * Cancel active trade.
	 */
	public void cancelActiveTrade()
	{
		if (_activeTradeList == null)
		{
			return;
		}
		
		L2PcInstance partner = _activeTradeList.getPartner();
		if (partner != null)
		{
			partner.onTradeCancel(this);
		}
		onTradeCancel(this);
	}
	
	/**
	 * Return the _createList object of the L2PcInstance.
	 * @return the creates the list
	 */
	public L2ManufactureList getCreateList()
	{
		return _createList;
	}
	
	/**
	 * Set the _createList object of the L2PcInstance.
	 * @param x the new creates the list
	 */
	public void setCreateList(L2ManufactureList x)
	{
		_createList = x;
	}
	
	/**
	 * Return the _buyList object of the L2PcInstance.
	 * @return the sell list
	 */
	public TradeList getSellList()
	{
		if (_sellList == null)
		{
			_sellList = new TradeList(this);
		}
		return _sellList;
	}
	
	/**
	 * Return the _buyList object of the L2PcInstance.
	 * @return the buy list
	 */
	public TradeList getBuyList()
	{
		if (_buyList == null)
		{
			_buyList = new TradeList(this);
		}
		return _buyList;
	}
	
	/**
	 * Set the Private Store type of the L2PcInstance.<BR>
	 * <B><U> Values </U> :</B><BR>
	 * <li>0 : STORE_PRIVATE_NONE</li> <li>1 : STORE_PRIVATE_SELL</li> <li>2 : sellmanage</li><BR>
	 * <li>3 : STORE_PRIVATE_BUY</li><BR>
	 * <li>4 : buymanage</li><BR>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li><BR>
	 * @param type the new private store type
	 */
	public void setPrivateStoreType(int type)
	{
		_privatestore = type;
	}
	
	/**
	 * Return the Private Store type of the L2PcInstance.<BR>
	 * <B><U> Values </U> :</B><BR>
	 * <li>0 : STORE_PRIVATE_NONE</li> <li>1 : STORE_PRIVATE_SELL</li> <li>2 : sellmanage</li><BR>
	 * <li>3 : STORE_PRIVATE_BUY</li><BR>
	 * <li>4 : buymanage</li><BR>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li><BR>
	 * @return the private store type
	 */
	public int getPrivateStoreType()
	{
		return _privatestore;
	}
	
	/**
	 * Set the _skillLearningClassId object of the L2PcInstance.
	 * @param classId the new skill learning class id
	 */
	public void setSkillLearningClassId(ClassId classId)
	{
		_skillLearningClassId = classId;
	}
	
	/**
	 * Return the _skillLearningClassId object of the L2PcInstance.
	 * @return the skill learning class id
	 */
	public ClassId getSkillLearningClassId()
	{
		return _skillLearningClassId;
	}
	
	/**
	 * Set the _clan object, _clanId, _clanLeader Flag and title of the L2PcInstance.
	 * @param clan the new clan
	 */
	public void setClan(L2Clan clan)
	{
		_clan = clan;
		setTitle("");
		
		if (clan == null)
		{
			_clanId = 0;
			_clanPrivileges = 0;
			_pledgeType = 0;
			_powerGrade = 0;
			_lvlJoinedAcademy = 0;
			_apprentice = 0;
			_sponsor = 0;
			return;
		}
		
		if (!clan.isMember(getName()))
		{
			// char has been kicked from clan
			setClan(null);
			return;
		}
		
		_clanId = clan.getClanId();
	}
	
	/**
	 * Return the _clan object of the L2PcInstance.
	 * @return the clan
	 */
	public L2Clan getClan()
	{
		return _clan;
	}
	
	/**
	 * Return True if the L2PcInstance is the leader of its clan.
	 * @return true, if is clan leader
	 */
	public boolean isClanLeader()
	{
		if (getClan() == null)
		{
			return false;
		}
		return getObjectId() == getClan().getLeaderId();
	}
	
	/**
	 * Reduce the number of arrows owned by the L2PcInstance and send it Server->Client Packet InventoryUpdate or ItemList (to unequip if the last arrow was consummed).
	 */
	@Override
	protected void reduceArrowCount()
	{
		L2ItemInstance arrows = getInventory().destroyItem("Consume", getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND), 1, this, null);
		
		if (Config.DEBUG)
		{
			_log.fine("arrow count:" + (arrows == null ? 0 : arrows.getCount()));
		}
		
		if ((arrows == null) || (arrows.getCount() == 0))
		{
			getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LHAND);
			_arrowItem = null;
			
			if (Config.DEBUG)
			{
				_log.fine("removed arrows count");
			}
			sendPacket(new ItemList(this, false));
		}
		else
		{
			if (!Config.FORCE_INVENTORY_UPDATE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(arrows);
				sendPacket(iu);
			}
			else
			{
				sendPacket(new ItemList(this, false));
			}
		}
	}
	
	/**
	 * Equip arrows needed in left hand and send a Server->Client packet ItemList to the L2PcINstance then return True.
	 * @return true, if successful
	 */
	@Override
	protected boolean checkAndEquipArrows()
	{
		// Check if nothing is equiped in left hand
		if (getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
		{
			// Get the L2ItemInstance of the arrows needed for this bow
			_arrowItem = getInventory().findArrowForBow(getActiveWeaponItem());
			
			if (_arrowItem != null)
			{
				// Equip arrows needed in left hand
				getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, _arrowItem);
				
				// Send a Server->Client packet ItemList to this L2PcINstance to update left hand equipement
				ItemList il = new ItemList(this, false);
				sendPacket(il);
			}
		}
		else
		{
			// Get the L2ItemInstance of arrows equiped in left hand
			_arrowItem = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		}
		
		return _arrowItem != null;
	}
	
	/**
	 * Disarm the player's weapon and shield.
	 * @return true, if successful
	 */
	public boolean disarmWeapons()
	{
		// Don't allow disarming a cursed weapon
		if (isCursedWeaponEquiped())
		{
			return false;
		}
		
		// Unequip the weapon
		L2ItemInstance wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (wpn == null)
		{
			wpn = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
		}
		if (wpn != null)
		{
			if (wpn.isWear())
			{
				return false;
			}
			
			// Remove augementation boni on unequip
			if (wpn.isAugmented())
			{
				wpn.getAugmentation().removeBoni(this);
			}
			
			L2ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance element : unequiped)
			{
				iu.addModifiedItem(element);
			}
			sendPacket(iu);
			
			abortAttack();
			broadcastUserInfo();
			
			// this can be 0 if the user pressed the right mousebutton twice very fast
			if (unequiped.length > 0)
			{
				SystemMessage sm = null;
				if (unequiped[0].getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(unequiped[0].getEnchantLevel());
					sm.addItemName(unequiped[0].getItemId());
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_DISARMED);
					sm.addItemName(unequiped[0].getItemId());
				}
				sendPacket(sm);
			}
		}
		
		// Unequip the shield
		L2ItemInstance sld = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (sld != null)
		{
			if (sld.isWear())
			{
				return false;
			}
			
			L2ItemInstance[] unequiped = getInventory().unEquipItemInBodySlotAndRecord(sld.getItem().getBodyPart());
			InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance element : unequiped)
			{
				iu.addModifiedItem(element);
			}
			sendPacket(iu);
			
			abortAttack();
			broadcastUserInfo();
			
			// this can be 0 if the user pressed the right mousebutton twice very fast
			if (unequiped.length > 0)
			{
				SystemMessage sm = null;
				if (unequiped[0].getEnchantLevel() > 0)
				{
					sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
					sm.addNumber(unequiped[0].getEnchantLevel());
					sm.addItemName(unequiped[0].getItemId());
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_DISARMED);
					sm.addItemName(unequiped[0].getItemId());
				}
				sendPacket(sm);
			}
		}
		return true;
	}
	
	/**
	 * Return True if the L2PcInstance use a dual weapon.
	 * @return true, if is using dual weapon
	 */
	@Override
	public boolean isUsingDualWeapon()
	{
		L2Weapon weaponItem = getActiveWeaponItem();
		if (weaponItem == null)
		{
			return false;
		}
		
		if (weaponItem.getItemType() == L2WeaponType.DUAL)
		{
			return true;
		}
		else if (weaponItem.getItemType() == L2WeaponType.DUALFIST)
		{
			return true;
		}
		else if (weaponItem.getItemId() == 248)
		{
			return true;
		}
		else if (weaponItem.getItemId() == 252)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Sets the uptime.
	 * @param time the new uptime
	 */
	public void setUptime(long time)
	{
		_uptime = time;
	}
	
	/**
	 * Gets the uptime.
	 * @return the uptime
	 */
	public long getUptime()
	{
		return System.currentTimeMillis() - _uptime;
	}
	
	/**
	 * Return True if the L2PcInstance is invulnerable.
	 * @return true, if is invul
	 */
	@Override
	public boolean isInvul()
	{
		return _isInvul || _isTeleporting || (_protectEndTime > GameTimeController.getGameTicks());
	}
	
	/**
	 * Return True if the L2PcInstance has a Party in progress.
	 * @return true, if is in party
	 */
	@Override
	public boolean isInParty()
	{
		return _party != null;
	}
	
	/**
	 * Set the _party object of the L2PcInstance (without joining it).
	 * @param party the new party
	 */
	public void setParty(L2Party party)
	{
		_party = party;
	}
	
	/**
	 * Set the _party object of the L2PcInstance AND join it.
	 * @param party the party
	 */
	public void joinParty(L2Party party)
	{
		if (party != null)
		{
			// First set the party otherwise this wouldn't be considered
			// as in a party into the L2Character.updateEffectIcons() call.
			_party = party;
			party.addPartyMember(this);
		}
	}
	
	/**
	 * Manage the Leave Party task of the L2PcInstance.
	 */
	public void leaveParty()
	{
		if (isInParty())
		{
			_party.removePartyMember(this);
			_party = null;
		}
	}
	
	/**
	 * Return the _party object of the L2PcInstance.
	 * @return the party
	 */
	@Override
	public L2Party getParty()
	{
		return _party;
	}
	
	/**
	 * Set the _isGm Flag of the L2PcInstance.
	 * @param status the new checks if is gm
	 */
	public void setIsGM(boolean status)
	{
		_isGm = status;
	}
	
	/**
	 * Return True if the L2PcInstance is a GM.
	 * @return true, if is gM
	 */
	public boolean isGM()
	{
		return _isGm;
	}
	
	/**
	 * Manage a cancel cast task for the L2PcInstance.<br>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>Set the Intention of the AI to AI_INTENTION_IDLE</li> <li>Enable all skills (set _allSkillsDisabled to False)</li> <li>Send a Server->Client Packet MagicSkillCanceld to the L2PcInstance and all L2PcInstance in the _KnownPlayers of the L2Character (broadcast)</li><BR>
	 */
	public void cancelCastMagic()
	{
		// Set the Intention of the AI to AI_INTENTION_IDLE
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		
		// Enable all skills (set _allSkillsDisabled to False)
		enableAllSkills();
		
		// Send a Server->Client Packet MagicSkillCanceld to the L2PcInstance and all L2PcInstance in the _KnownPlayers of the L2Character (broadcast)
		MagicSkillCanceld msc = new MagicSkillCanceld(getObjectId());
		
		// Broadcast the packet to self and known players.
		Broadcast.toSelfAndKnownPlayersInRadius(this, msc, 810000/* 900 */);
	}
	
	/**
	 * Set the _accessLevel of the L2PcInstance.
	 * @param level the new access level
	 */
	public void setAccessLevel(int level)
	{
		_accessLevel = level;
		
		if ((_accessLevel > 0) || Config.EVERYBODY_HAS_ADMIN_RIGHTS)
		{
			setIsGM(true);
		}
	}
	
	/**
	 * Sets the account accesslevel.
	 * @param level the new account accesslevel
	 */
	public void setAccountAccesslevel(int level)
	{
		LoginServerThread.getInstance().sendAccessLevel(getAccountName(), level);
	}
	
	/**
	 * Return the _accessLevel of the L2PcInstance.
	 * @return the access level
	 */
	public int getAccessLevel()
	{
		if (Config.EVERYBODY_HAS_ADMIN_RIGHTS && (_accessLevel <= 200))
		{
			return 200;
		}
		
		return _accessLevel;
	}
	
	@Override
	public double getLevelMod()
	{
		return ((100.0 - 11) + getLevel()) / 100.0;
	}
	
	/**
	 * Update Stats of the L2PcInstance client side by sending Server->Client packet UserInfo/StatusUpdate to this L2PcInstance and CharInfo/StatusUpdate to all L2PcInstance in its _KnownPlayers (broadcast).
	 * @param broadcastType the broadcast type
	 */
	public void updateAndBroadcastStatus(int broadcastType)
	{
		refreshOverloaded();
		refreshExpertisePenalty();
		// Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers (broadcast)
		if (broadcastType == 1)
		{
			sendPacket(new UserInfo(this));
		}
		if (broadcastType == 2)
		{
			broadcastUserInfo();
		}
	}
	
	/**
	 * Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2PcInstance and all L2PcInstance to inform (broadcast).
	 * @param flag the new karma flag
	 */
	public void setKarmaFlag(int flag)
	{
		sendPacket(new UserInfo(this));
		for (L2PcInstance player : getKnownList().getKnownPlayers().values())
		{
			player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
		}
	}
	
	/**
	 * Send a Server->Client StatusUpdate packet with Karma to the L2PcInstance and all L2PcInstance to inform (broadcast).
	 */
	public void broadcastKarma()
	{
		sendPacket(new UserInfo(this));
		for (L2PcInstance player : getKnownList().getKnownPlayers().values())
		{
			player.sendPacket(new RelationChanged(this, getRelation(player), isAutoAttackable(player)));
		}
	}
	
	/**
	 * Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout).
	 * @param isOnline the new online status
	 */
	public void setOnlineStatus(boolean isOnline)
	{
		if (_isOnline != isOnline)
		{
			_isOnline = isOnline;
		}
		
		// Update the characters table of the database with online status and lastAccess (called when login and logout)
		updateOnlineStatus();
	}
	
	/**
	 * Sets the checks if is in7s dungeon.
	 * @param isIn7sDungeon the new checks if is in7s dungeon
	 */
	public void setIsIn7sDungeon(boolean isIn7sDungeon)
	{
		if (_isIn7sDungeon != isIn7sDungeon)
		{
			_isIn7sDungeon = isIn7sDungeon;
		}
		
		updateIsIn7sDungeonStatus();
	}
	
	/**
	 * Update the characters table of the database with online status and lastAccess of this L2PcInstance (called when login and logout).
	 */
	public void updateOnlineStatus()
	{
		java.sql.Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET online=?, lastAccess=? WHERE obj_id=?");
			statement.setInt(1, isOnline());
			statement.setLong(2, System.currentTimeMillis());
			statement.setInt(3, getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("could not set char online status:" + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	/**
	 * Update is in7s dungeon status.
	 */
	public void updateIsIn7sDungeonStatus()
	{
		java.sql.Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE characters SET isIn7sDungeon=?, lastAccess=? WHERE obj_id=?");
			statement.setInt(1, isIn7sDungeon() ? 1 : 0);
			statement.setLong(2, System.currentTimeMillis());
			statement.setInt(3, getObjectId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("could not set char isIn7sDungeon status:" + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	/**
	 * Create a new player in the characters table of the database.
	 * @return true, if successful
	 */
	private boolean createDb()
	{
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("INSERT INTO characters " + "(account_name,obj_Id,char_name,level,maxHp,curHp,maxCp,curCp,maxMp,curMp," + "acc,crit,evasion,mAtk,mDef,mSpd,pAtk,pDef,pSpd,runSpd,walkSpd," + "str,con,dex,_int,men,wit,face,hairStyle,hairColor,sex," + "movement_multiplier,attack_speed_multiplier,colRad,colHeight," + "exp,sp,karma,pvpkills,pkkills,clanid,maxload,race,classid,deletetime," + "cancraft,title,accesslevel,online,isin7sdungeon,clan_privs,wantspeace," + "base_class,newbie,nobless,power_grade,last_recom_date) " + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			statement.setString(1, _accountName);
			statement.setInt(2, getObjectId());
			statement.setString(3, getName());
			statement.setInt(4, getLevel());
			statement.setInt(5, getMaxHp());
			statement.setDouble(6, getCurrentHp());
			statement.setInt(7, getMaxCp());
			statement.setDouble(8, getCurrentCp());
			statement.setInt(9, getMaxMp());
			statement.setDouble(10, getCurrentMp());
			statement.setInt(11, getAccuracy());
			statement.setInt(12, getCriticalHit(null, null));
			statement.setInt(13, getEvasionRate(null));
			statement.setInt(14, getMAtk(null, null));
			statement.setInt(15, getMDef(null, null));
			statement.setInt(16, getMAtkSpd());
			statement.setInt(17, getPAtk(null));
			statement.setInt(18, getPDef(null));
			statement.setInt(19, getPAtkSpd());
			statement.setInt(20, getRunSpeed());
			statement.setInt(21, getWalkSpeed());
			statement.setInt(22, getSTR());
			statement.setInt(23, getCON());
			statement.setInt(24, getDEX());
			statement.setInt(25, getINT());
			statement.setInt(26, getMEN());
			statement.setInt(27, getWIT());
			statement.setInt(28, getAppearance().getFace());
			statement.setInt(29, getAppearance().getHairStyle());
			statement.setInt(30, getAppearance().getHairColor());
			statement.setInt(31, getAppearance().getSex() ? 1 : 0);
			statement.setDouble(32, 1/* getMovementMultiplier() */);
			statement.setDouble(33, 1/* getAttackSpeedMultiplier() */);
			statement.setDouble(34, getTemplate().collisionRadius/* getCollisionRadius() */);
			statement.setDouble(35, getTemplate().collisionHeight/* getCollisionHeight() */);
			statement.setLong(36, getExp());
			statement.setInt(37, getSp());
			statement.setInt(38, getKarma());
			statement.setInt(39, getPvpKills());
			statement.setInt(40, getPkKills());
			statement.setInt(41, getClanId());
			statement.setInt(42, getMaxLoad());
			statement.setInt(43, getRace().ordinal());
			statement.setInt(44, getClassId().getId());
			statement.setLong(45, getDeleteTimer());
			statement.setInt(46, hasDwarvenCraft() ? 1 : 0);
			statement.setString(47, getTitle());
			statement.setInt(48, getAccessLevel());
			statement.setInt(49, isOnline());
			statement.setInt(50, isIn7sDungeon() ? 1 : 0);
			statement.setInt(51, getClanPrivileges());
			statement.setInt(52, getWantsPeace());
			statement.setInt(53, getBaseClass());
			statement.setInt(54, isNewbie() ? 1 : 0);
			statement.setInt(55, isNoble() ? 1 : 0);
			statement.setLong(56, 0);
			statement.setLong(57, System.currentTimeMillis());
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.severe("Could not insert char data: " + e);
			return false;
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
		return true;
	}
	
	/**
	 * Retrieve a L2PcInstance from the characters table of the database and add it in _allObjects of the L2world.<BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>Retrieve the L2PcInstance from the characters table of the database</li> <li>Add the L2PcInstance object in _allObjects</li> <li>Set the x,y,z position of the L2PcInstance and make it invisible</li> <li>Update the overloaded status of the L2PcInstance</li><BR>
	 * @param objectId Identifier of the object to initialized
	 * @return The L2PcInstance loaded from the database
	 */
	private static L2PcInstance restore(int objectId)
	{
		L2PcInstance player = null;
		java.sql.Connection con = null;
		
		try
		{
			// Retrieve the L2PcInstance from the characters table of the database
			con = L2DatabaseFactory.getInstance().getConnection();
			
			PreparedStatement statement = con.prepareStatement(RESTORE_CHARACTER);
			statement.setInt(1, objectId);
			ResultSet rset = statement.executeQuery();
			
			double currentCp = 0;
			double currentHp = 0;
			double currentMp = 0;
			
			while (rset.next())
			{
				final int activeClassId = rset.getInt("classid");
				final boolean female = rset.getInt("sex") != 0;
				final L2PcTemplate template = CharTemplateTable.getInstance().getTemplate(activeClassId);
				PcAppearance app = new PcAppearance(rset.getByte("face"), rset.getByte("hairColor"), rset.getByte("hairStyle"), female);
				
				player = new L2PcInstance(objectId, template, rset.getString("account_name"), app);
				player.setName(rset.getString("char_name"));
				player._lastAccess = rset.getLong("lastAccess");
				
				player.getStat().setExp(rset.getLong("exp"));
				player.setExpBeforeDeath(rset.getLong("expBeforeDeath"));
				player.getStat().setLevel(rset.getByte("level"));
				player.getStat().setSp(rset.getInt("sp"));
				
				player.setWantsPeace(rset.getInt("wantspeace"));
				
				player.setHeading(rset.getInt("heading"));
				
				player.setKarma(rset.getInt("karma"));
				player.setPvpKills(rset.getInt("pvpkills"));
				player.setPkKills(rset.getInt("pkkills"));
				player.setOnlineTime(rset.getLong("onlinetime"));
				player.setNewbie(rset.getInt("newbie") == 1);
				player.setNoble(rset.getInt("nobless") == 1);
				
				player.setClanJoinExpiryTime(rset.getLong("clan_join_expiry_time"));
				if (player.getClanJoinExpiryTime() < System.currentTimeMillis())
				{
					player.setClanJoinExpiryTime(0);
				}
				player.setClanCreateExpiryTime(rset.getLong("clan_create_expiry_time"));
				if (player.getClanCreateExpiryTime() < System.currentTimeMillis())
				{
					player.setClanCreateExpiryTime(0);
				}
				
				int clanId = rset.getInt("clanid");
				player.setPowerGrade((int) rset.getLong("power_grade"));
				player.setPledgeType(rset.getInt("subpledge"));
				player.setLastRecomUpdate(rset.getLong("last_recom_date"));
				// player.setApprentice(rset.getInt("apprentice"));
				
				if (clanId > 0)
				{
					player.setClan(ClanTable.getInstance().getClan(clanId));
				}
				
				if (player.getClan() != null)
				{
					if (player.getClan().getLeaderId() != player.getObjectId())
					{
						if (player.getPowerGrade() == 0)
						{
							player.setPowerGrade(5);
						}
						player.setClanPrivileges(player.getClan().getRankPrivs(player.getPowerGrade()));
					}
					else
					{
						player.setClanPrivileges(L2Clan.CP_ALL);
						player.setPowerGrade(1);
					}
				}
				else
				{
					player.setClanPrivileges(L2Clan.CP_NOTHING);
				}
				
				player.setDeleteTimer(rset.getLong("deletetime"));
				
				player.setTitle(rset.getString("title"));
				player.setAccessLevel(rset.getInt("accesslevel"));
				player.setFistsWeaponItem(player.findFistsWeaponItem(activeClassId));
				player.setUptime(System.currentTimeMillis());
				
				currentHp = rset.getDouble("curHp");
				player.setCurrentHp(rset.getDouble("curHp"));
				currentCp = rset.getDouble("curCp");
				player.setCurrentCp(rset.getDouble("curCp"));
				currentMp = rset.getDouble("curMp");
				player.setCurrentMp(rset.getDouble("curMp"));
				
				// Check recs
				player.checkRecom(rset.getInt("rec_have"), rset.getInt("rec_left"));
				
				player._classIndex = 0;
				try
				{
					player.setBaseClass(rset.getInt("base_class"));
				}
				catch (Exception e)
				{
					player.setBaseClass(activeClassId);
				}
				
				// Restore Subclass Data (cannot be done earlier in function)
				if (restoreSubClassData(player))
				{
					if (activeClassId != player.getBaseClass())
					{
						for (SubClass subClass : player.getSubClasses().values())
						{
							if (subClass.getClassId() == activeClassId)
							{
								player._classIndex = subClass.getClassIndex();
							}
						}
					}
				}
				if ((player.getClassIndex() == 0) && (activeClassId != player.getBaseClass()))
				{
					// Subclass in use but doesn't exist in DB -
					// a possible restart-while-modifysubclass cheat has been attempted.
					// Switching to use base class
					player.setClassId(player.getBaseClass());
					_log.warning("Player " + player.getName() + " reverted to base class. Possibly has tried a relogin exploit while subclassing.");
				}
				else
				{
					player._activeClass = activeClassId;
				}
				
				player.setApprentice(rset.getInt("apprentice"));
				player.setSponsor(rset.getInt("sponsor"));
				player.setLvlJoinedAcademy(rset.getInt("lvl_joined_academy"));
				player.setIsIn7sDungeon((rset.getInt("isin7sdungeon") == 1) ? true : false);
				player.setInJail((rset.getInt("in_jail") == 1) ? true : false);
				if (player.isInJail())
				{
					player.setJailTimer(rset.getLong("jail_timer"));
				}
				else
				{
					player.setJailTimer(0);
				}
				
				CursedWeaponsManager.getInstance().checkPlayer(player);
				
				player.setAllianceWithVarkaKetra(rset.getInt("varka_ketra_ally"));
				
				player.setDeathPenaltyBuffLevel(rset.getInt("death_penalty_level"));
				
				// Add the L2PcInstance object in _allObjects
				// L2World.getInstance().storeObject(player);
				
				// Set the x,y,z position of the L2PcInstance and make it invisible
				player.setXYZInvisible(rset.getInt("x"), rset.getInt("y"), rset.getInt("z"));
				
				// Retrieve the name and ID of the other characters assigned to this account.
				PreparedStatement stmt = con.prepareStatement("SELECT obj_Id, char_name FROM characters WHERE account_name=? AND obj_Id<>?");
				stmt.setString(1, player._accountName);
				stmt.setInt(2, objectId);
				ResultSet chars = stmt.executeQuery();
				
				while (chars.next())
				{
					Integer charId = chars.getInt("obj_Id");
					String charName = chars.getString("char_name");
					player._chars.put(charId, charName);
				}
				
				chars.close();
				stmt.close();
				break;
			}
			
			rset.close();
			statement.close();
			
			if (player == null)
			{
				_log.warning("Couldn't restore player from database!!!");
				return null;
			}
			
			// Retrieve from the database all secondary data of this L2PcInstance
			// and reward expertise/lucky skills if necessary.
			// Note that Clan, Noblesse and Hero skills are given separately and not here.
			player.restoreCharData();
			player.rewardSkills();
			
			// Restore current Cp, HP and MP values
			player.setCurrentCp(currentCp);
			player.setCurrentHp(currentHp);
			player.setCurrentMp(currentMp);
			
			// Restore pet if exists in the world
			player.setPet(L2World.getInstance().getPet(player.getObjectId()));
			if (player.getPet() != null)
			{
				player.getPet().setOwner(player);
			}
			
			// Update the overloaded status of the L2PcInstance
			player.refreshOverloaded();
		}
		catch (Exception e)
		{
			_log.severe("Could not restore char data: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
		
		return player;
	}
	
	/**
	 * Gets the mail.
	 * @return the mail
	 */
	public Forum getMail()
	{
		if (_forumMail == null)
		{
			setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName()));
			
			if (_forumMail == null)
			{
				ForumsBBSManager.getInstance().createNewForum(getName(), ForumsBBSManager.getInstance().getForumByName("MailRoot"), Forum.MAIL, Forum.OWNERONLY, getObjectId());
				setMail(ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName()));
			}
		}
		
		return _forumMail;
	}
	
	/**
	 * Sets the mail.
	 * @param forum the new mail
	 */
	public void setMail(Forum forum)
	{
		_forumMail = forum;
	}
	
	/**
	 * Gets the memo.
	 * @return the memo
	 */
	public Forum getMemo()
	{
		if (_forumMemo == null)
		{
			setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));
			
			if (_forumMemo == null)
			{
				ForumsBBSManager.getInstance().createNewForum(_accountName, ForumsBBSManager.getInstance().getForumByName("MemoRoot"), Forum.MEMO, Forum.OWNERONLY, getObjectId());
				setMemo(ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(_accountName));
			}
		}
		
		return _forumMemo;
	}
	
	/**
	 * Sets the memo.
	 * @param forum the new memo
	 */
	public void setMemo(Forum forum)
	{
		_forumMemo = forum;
	}
	
	/**
	 * Restores sub-class data for the L2PcInstance, used to check the current class index for the character.
	 * @param player the player
	 * @return true, if successful
	 */
	private static boolean restoreSubClassData(L2PcInstance player)
	{
		java.sql.Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_SUBCLASSES);
			statement.setInt(1, player.getObjectId());
			
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				SubClass subClass = new SubClass();
				subClass.setClassId(rset.getInt("class_id"));
				subClass.setLevel(rset.getByte("level"));
				subClass.setExp(rset.getLong("exp"));
				subClass.setSp(rset.getInt("sp"));
				subClass.setClassIndex(rset.getInt("class_index"));
				
				// Enforce the correct indexing of _subClasses against their class indexes.
				player.getSubClasses().put(subClass.getClassIndex(), subClass);
			}
			
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not restore classes for " + player.getName() + ": " + e);
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
		
		return true;
	}
	
	/**
	 * Restores secondary data for the L2PcInstance, based on the current class index.
	 */
	private void restoreCharData()
	{
		// Retrieve from the database all skills of this L2PcInstance and add them to _skills.
		restoreSkills();
		
		// Retrieve from the database all macroses of this L2PcInstance and add them to _macroses.
		_macroses.restore();
		
		// Retrieve from the database all shortCuts of this L2PcInstance and add them to _shortCuts.
		_shortCuts.restore();
		
		// Retrieve from the database all henna of this L2PcInstance and add them to _henna.
		restoreHenna();
		
		// Retrieve from the database all recom data of this L2PcInstance and add to _recomChars.
		if (Config.ALT_RECOMMEND)
		{
			restoreRecom();
		}
		
		// Retrieve from the database the recipe book of this L2PcInstance.
		if (!isSubClassActive())
		{
			restoreRecipeBook();
		}
	}
	
	/**
	 * Store recipe book data for this L2PcInstance, if not on an active sub-class.
	 */
	private void storeRecipeBook()
	{
		// If the player is on a sub-class don't even attempt to store a recipe book.
		if (isSubClassActive())
		{
			return;
		}
		if ((getCommonRecipeBook().length == 0) && (getDwarvenRecipeBook().length == 0))
		{
			return;
		}
		
		java.sql.Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM character_recipebook WHERE char_id=?");
			statement.setInt(1, getObjectId());
			statement.execute();
			statement.close();
			
			L2RecipeList[] recipes = getCommonRecipeBook();
			
			for (L2RecipeList recipe : recipes)
			{
				statement = con.prepareStatement("INSERT INTO character_recipebook (char_id, id, type) values(?,?,0)");
				statement.setInt(1, getObjectId());
				statement.setInt(2, recipe.getId());
				statement.execute();
				statement.close();
			}
			
			recipes = getDwarvenRecipeBook();
			for (L2RecipeList recipe : recipes)
			{
				statement = con.prepareStatement("INSERT INTO character_recipebook (char_id, id, type) values(?,?,1)");
				statement.setInt(1, getObjectId());
				statement.setInt(2, recipe.getId());
				statement.execute();
				statement.close();
			}
		}
		catch (Exception e)
		{
			_log.warning("Could not store recipe book data: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	/**
	 * Restore recipe book data for this L2PcInstance.
	 */
	private void restoreRecipeBook()
	{
		java.sql.Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT id, type FROM character_recipebook WHERE char_id=?");
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			
			L2RecipeList recipe;
			while (rset.next())
			{
				recipe = RecipeController.getInstance().getRecipeList(rset.getInt("id") - 1);
				
				if (rset.getInt("type") == 1)
				{
					registerDwarvenRecipeList(recipe);
				}
				else
				{
					registerCommonRecipeList(recipe);
				}
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not restore recipe book data:" + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	/**
	 * Update L2PcInstance stats in the characters table of the database.
	 */
	public synchronized void store()
	{
		// update client coords, if these look like true
		if (isInsideRadius(getClientX(), getClientY(), 1000, true))
		{
			setXYZ(getClientX(), getClientY(), getClientZ());
		}
		
		storeCharBase();
		storeCharSub();
		storeEffect();
		storeRecipeBook();
	}
	
	/**
	 * Store char base.
	 */
	private void storeCharBase()
	{
		java.sql.Connection con = null;
		
		try
		{
			// Get the exp, level, and sp of base class to store in base table
			int currentClassIndex = getClassIndex();
			_classIndex = 0;
			long exp = getStat().getExp();
			int level = getStat().getLevel();
			int sp = getStat().getSp();
			_classIndex = currentClassIndex;
			
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			// Update base class
			statement = con.prepareStatement(UPDATE_CHARACTER);
			statement.setInt(1, level);
			statement.setInt(2, getMaxHp());
			statement.setDouble(3, getCurrentHp());
			statement.setInt(4, getMaxCp());
			statement.setDouble(5, getCurrentCp());
			statement.setInt(6, getMaxMp());
			statement.setDouble(7, getCurrentMp());
			statement.setInt(8, getSTR());
			statement.setInt(9, getCON());
			statement.setInt(10, getDEX());
			statement.setInt(11, getINT());
			statement.setInt(12, getMEN());
			statement.setInt(13, getWIT());
			statement.setInt(14, getAppearance().getFace());
			statement.setInt(15, getAppearance().getHairStyle());
			statement.setInt(16, getAppearance().getHairColor());
			statement.setInt(17, getHeading());
			statement.setInt(18, _observerMode ? _obsX : getX());
			statement.setInt(19, _observerMode ? _obsY : getY());
			statement.setInt(20, _observerMode ? _obsZ : getZ());
			statement.setLong(21, exp);
			statement.setLong(22, getExpBeforeDeath());
			statement.setInt(23, sp);
			statement.setInt(24, getKarma());
			statement.setInt(25, getPvpKills());
			statement.setInt(26, getPkKills());
			statement.setInt(27, getRecomHave());
			statement.setInt(28, getRecomLeft());
			statement.setInt(29, getClanId());
			statement.setInt(30, getMaxLoad());
			statement.setInt(31, getRace().ordinal());
			
			// if (!isSubClassActive())
			
			// else
			// statement.setInt(30, getBaseTemplate().race.ordinal());
			
			statement.setInt(32, getClassId().getId());
			statement.setLong(33, getDeleteTimer());
			statement.setString(34, getTitle());
			statement.setInt(35, getAccessLevel());
			statement.setInt(36, isOnline());
			statement.setInt(37, isIn7sDungeon() ? 1 : 0);
			statement.setInt(38, getClanPrivileges());
			statement.setInt(39, getWantsPeace());
			statement.setInt(40, getBaseClass());
			
			long totalOnlineTime = _onlineTime;
			
			if (_onlineBeginTime > 0)
			{
				totalOnlineTime += (System.currentTimeMillis() - _onlineBeginTime) / 1000;
			}
			
			statement.setLong(41, totalOnlineTime);
			statement.setInt(42, isInJail() ? 1 : 0);
			statement.setLong(43, getJailTimer());
			statement.setInt(44, isNewbie() ? 1 : 0);
			statement.setInt(45, isNoble() ? 1 : 0);
			statement.setLong(46, getPowerGrade());
			statement.setInt(47, getPledgeType());
			statement.setLong(48, getLastRecomUpdate());
			statement.setInt(49, getLvlJoinedAcademy());
			statement.setLong(50, getApprentice());
			statement.setLong(51, getSponsor());
			statement.setInt(52, getAllianceWithVarkaKetra());
			statement.setLong(53, getClanJoinExpiryTime());
			statement.setLong(54, getClanCreateExpiryTime());
			statement.setString(55, getName());
			statement.setLong(56, getDeathPenaltyBuffLevel());
			statement.setInt(57, getObjectId());
			
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not store char base data: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	/**
	 * Store char sub.
	 */
	private void storeCharSub()
	{
		java.sql.Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			if (getTotalSubClasses() > 0)
			{
				for (SubClass subClass : getSubClasses().values())
				{
					statement = con.prepareStatement(UPDATE_CHAR_SUBCLASS);
					statement.setLong(1, subClass.getExp());
					statement.setInt(2, subClass.getSp());
					statement.setInt(3, subClass.getLevel());
					statement.setInt(4, subClass.getClassId());
					statement.setInt(5, getObjectId());
					statement.setInt(6, subClass.getClassIndex());
					
					statement.execute();
					statement.close();
				}
			}
		}
		catch (Exception e)
		{
			_log.warning("Could not store sub class data for " + getName() + ": " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	/**
	 * Store effect.
	 */
	private void storeEffect()
	{
		if (!Config.STORE_SKILL_COOLTIME)
		{
			return;
		}
		
		java.sql.Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			// Delete all current stored effects for char to avoid dupe
			statement = con.prepareStatement(DELETE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.execute();
			statement.close();
			
			int buff_index = 0;
			
			// Store all effect data along with calulated remaining
			// reuse delays for matching skills. 'restore_type'= 0.
			for (L2Effect effect : getAllEffects())
			{
				if ((effect != null) && effect.getInUse() && !effect.getSkill().isToggle())
				{
					int skillId = effect.getSkill().getId();
					buff_index++;
					
					statement = con.prepareStatement(ADD_SKILL_SAVE);
					statement.setInt(1, getObjectId());
					statement.setInt(2, skillId);
					statement.setInt(3, effect.getSkill().getLevel());
					statement.setInt(4, effect.getCount());
					statement.setInt(5, effect.getTime());
					
					if (ReuseTimeStamps.containsKey(skillId))
					{
						TimeStamp t = ReuseTimeStamps.remove(skillId);
						statement.setLong(6, t.hasNotPassed() ? t.getReuse() : 0);
					}
					else
					{
						statement.setLong(6, 0);
					}
					
					statement.setInt(7, 0);
					statement.setInt(8, getClassIndex());
					statement.setInt(9, buff_index);
					statement.execute();
					statement.close();
				}
			}
			
			// Store the reuse delays of remaining skills which
			// lost effect but still under reuse delay. 'restore_type' 1.
			for (TimeStamp t : ReuseTimeStamps.values())
			{
				if (t.hasNotPassed())
				{
					buff_index++;
					statement = con.prepareStatement(ADD_SKILL_SAVE);
					statement.setInt(1, getObjectId());
					statement.setInt(2, t.getSkill());
					statement.setInt(3, -1);
					statement.setInt(4, -1);
					statement.setInt(5, -1);
					statement.setLong(6, t.getReuse());
					statement.setInt(7, 1);
					statement.setInt(8, getClassIndex());
					statement.setInt(9, buff_index);
					statement.execute();
					statement.close();
				}
			}
			ReuseTimeStamps.clear();
		}
		catch (Exception e)
		{
			_log.warning("Could not store char effect data: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	/**
	 * Return True if the L2PcInstance is on line.
	 * @return the int
	 */
	public int isOnline()
	{
		return (_isOnline ? 1 : 0);
	}
	
	/**
	 * Checks if is in7s dungeon.
	 * @return true, if is in7s dungeon
	 */
	public boolean isIn7sDungeon()
	{
		return _isIn7sDungeon;
	}
	
	/**
	 * Add a skill to the L2PcInstance _skills and its Func objects to the calculator set of the L2PcInstance and save update in the character_skills table of the database. <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * All skills own by a L2PcInstance are identified in <B>_skills</B><BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>Replace oldSkill by newSkill or Add the newSkill</li> <li>If an old skill has been replaced, remove all its Func objects of L2Character calculator set</li> <li>Add Func objects of newSkill to the calculator set of the L2Character</li><BR>
	 * @param newSkill The L2Skill to add to the L2Character
	 * @param store the store
	 * @return The L2Skill replaced or null if just added a new L2Skill
	 */
	public L2Skill addSkill(L2Skill newSkill, boolean store)
	{
		// Add a skill to the L2PcInstance _skills and its Func objects to the calculator set of the L2PcInstance
		L2Skill oldSkill = super.addSkill(newSkill);
		
		// Add or update a L2PcInstance skill in the character_skills table of the database
		if (store)
		{
			storeSkill(newSkill, oldSkill, -1);
		}
		
		return oldSkill;
	}
	
	/**
	 * Removes the skill.
	 * @param skill the skill
	 * @param store the store
	 * @return the l2 skill
	 */
	public L2Skill removeSkill(L2Skill skill, boolean store)
	{
		if (store)
		{
			return removeSkill(skill);
		}
		return super.removeSkill(skill);
	}
	
	/**
	 * Remove a skill from the L2Character and its Func objects from calculator set of the L2Character and save update in the character_skills table of the database.<BR>
	 * <B><U> Concept</U> :</B><BR>
	 * All skills own by a L2Character are identified in <B>_skills</B><BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>Remove the skill from the L2Character _skills</li> <li>Remove all its Func objects from the L2Character calculator set</li><BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <li>L2PcInstance : Save update in the character_skills table of the database</li><BR>
	 * @param skill The L2Skill to remove from the L2Character
	 * @return The L2Skill removed
	 */
	@Override
	public L2Skill removeSkill(L2Skill skill)
	{
		// Remove a skill from the L2Character and its Func objects from calculator set of the L2Character
		L2Skill oldSkill = super.removeSkill(skill);
		
		java.sql.Connection con = null;
		
		try
		{
			// Remove or update a L2PcInstance skill from the character_skills table of the database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			if (oldSkill != null)
			{
				statement = con.prepareStatement(DELETE_SKILL_FROM_CHAR);
				statement.setInt(1, oldSkill.getId());
				statement.setInt(2, getObjectId());
				statement.setInt(3, getClassIndex());
				statement.execute();
				statement.close();
			}
		}
		catch (Exception e)
		{
			_log.warning("Error could not delete skill: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
		
		L2ShortCut[] allShortCuts = getAllShortCuts();
		
		for (L2ShortCut sc : allShortCuts)
		{
			if ((sc != null) && (skill != null) && (sc.getId() == skill.getId()) && (sc.getType() == L2ShortCut.TYPE_SKILL))
			{
				deleteShortCut(sc.getSlot(), sc.getPage());
			}
		}
		
		return oldSkill;
	}
	
	/**
	 * Add or update a L2PcInstance skill in the character_skills table of the database. <BR>
	 * If newClassIndex > -1, the skill will be stored with that class index, not the current one.
	 * @param newSkill the new skill
	 * @param oldSkill the old skill
	 * @param newClassIndex the new class index
	 */
	private void storeSkill(L2Skill newSkill, L2Skill oldSkill, int newClassIndex)
	{
		int classIndex = _classIndex;
		
		if (newClassIndex > -1)
		{
			classIndex = newClassIndex;
		}
		
		java.sql.Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			if ((oldSkill != null) && (newSkill != null))
			{
				statement = con.prepareStatement(UPDATE_CHARACTER_SKILL_LEVEL);
				statement.setInt(1, newSkill.getLevel());
				statement.setInt(2, oldSkill.getId());
				statement.setInt(3, getObjectId());
				statement.setInt(4, classIndex);
				statement.execute();
				statement.close();
			}
			else if (newSkill != null)
			{
				statement = con.prepareStatement(ADD_NEW_SKILL);
				statement.setInt(1, getObjectId());
				statement.setInt(2, newSkill.getId());
				statement.setInt(3, newSkill.getLevel());
				statement.setString(4, newSkill.getName());
				statement.setInt(5, classIndex);
				statement.execute();
				statement.close();
			}
			else
			{
				_log.warning("could not store new skill. its NULL");
			}
		}
		catch (Exception e)
		{
			_log.warning("Error could not store char skills: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	/**
	 * Retrieve from the database all skills of this L2PcInstance and add them to _skills.
	 */
	private void restoreSkills()
	{
		java.sql.Connection con = null;
		
		try
		{
			// Retrieve all skills of this L2PcInstance from the database
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			ResultSet rset = statement.executeQuery();
			
			// Go though the recordset of this SQL query
			while (rset.next())
			{
				int id = rset.getInt("skill_id");
				int level = rset.getInt("skill_level");
				
				if (id > 9000)
				{
					continue; // fake skills for base stats
				}
				
				// Create a L2Skill object for each record
				L2Skill skill = SkillTable.getInstance().getInfo(id, level);
				
				// Add the L2Skill object to the L2Character _skills and its Func objects to the calculator set of the L2Character
				super.addSkill(skill);
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not restore character skills: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	/**
	 * Retrieve from the database all skill effects of this L2PcInstance and add them to the player.
	 */
	public void restoreEffects()
	{
		L2Object[] targets = new L2Character[]
		{
			this
		};
		java.sql.Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			ResultSet rset;
			
			/**
			 * Restore Type 0 These skill were still in effect on the character upon logout. Some of which were self casted and might still have had a long reuse delay which also is restored.
			 */
			statement = con.prepareStatement(RESTORE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.setInt(3, 0);
			rset = statement.executeQuery();
			
			while (rset.next())
			{
				int skillId = rset.getInt("skill_id");
				int skillLvl = rset.getInt("skill_level");
				int effectCount = rset.getInt("effect_count");
				int effectCurTime = rset.getInt("effect_cur_time");
				long reuseDelay = rset.getLong("reuse_delay");
				
				// Just incase the admin minipulated this table incorrectly :x
				if ((skillId == -1) || (effectCount == -1) || (effectCurTime == -1) || (reuseDelay < 0))
				{
					continue;
				}
				
				L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLvl);
				ISkillHandler IHand = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
				if (IHand != null)
				{
					IHand.useSkill(this, skill, targets);
				}
				else
				{
					skill.useSkill(this, targets);
				}
				
				if (reuseDelay > 10)
				{
					disableSkill(skillId, reuseDelay);
					addTimeStamp(new TimeStamp(skillId, reuseDelay));
				}
				
				for (L2Effect effect : getAllEffects())
				{
					if (effect.getSkill().getId() == skillId)
					{
						effect.setCount(effectCount);
						effect.setFirstTime(effectCurTime);
					}
				}
			}
			rset.close();
			statement.close();
			
			/**
			 * Restore Type 1 The remaning skills lost effect upon logout but were still under a high reuse delay.
			 */
			statement = con.prepareStatement(RESTORE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.setInt(3, 1);
			rset = statement.executeQuery();
			
			while (rset.next())
			{
				int skillId = rset.getInt("skill_id");
				long reuseDelay = rset.getLong("reuse_delay");
				
				if (reuseDelay <= 0)
				{
					continue;
				}
				
				disableSkill(skillId, reuseDelay);
				addTimeStamp(new TimeStamp(skillId, reuseDelay));
			}
			rset.close();
			statement.close();
			
			statement = con.prepareStatement(DELETE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			statement.executeUpdate();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not restore active effect data: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
		
		updateEffectIcons();
	}
	
	/**
	 * Retrieve from the database all Henna of this L2PcInstance, add them to _henna and calculate stats of the L2PcInstance.
	 */
	private void restoreHenna()
	{
		java.sql.Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_HENNAS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, getClassIndex());
			ResultSet rset = statement.executeQuery();
			
			for (int i = 0; i < 3; i++)
			{
				_henna[i] = null;
			}
			
			while (rset.next())
			{
				int slot = rset.getInt("slot");
				
				if ((slot < 1) || (slot > 3))
				{
					continue;
				}
				
				int symbol_id = rset.getInt("symbol_id");
				
				L2HennaInstance sym = null;
				
				if (symbol_id != 0)
				{
					L2Henna tpl = HennaTable.getInstance().getTemplate(symbol_id);
					
					if (tpl != null)
					{
						sym = new L2HennaInstance(tpl);
						_henna[slot - 1] = sym;
					}
				}
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("could not restore henna: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
		
		// Calculate Henna modifiers of this L2PcInstance
		recalcHennaStats();
	}
	
	/**
	 * Retrieve from the database all Recommendation data of this L2PcInstance, add to _recomChars and calculate stats of the L2PcInstance.
	 */
	private void restoreRecom()
	{
		java.sql.Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_RECOMS);
			statement.setInt(1, getObjectId());
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				_recomChars.add(rset.getInt("target_id"));
			}
			
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("could not restore recommendations: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
	}
	
	/**
	 * Return the number of Henna empty slot of the L2PcInstance.
	 * @return the henna empty slots
	 */
	public int getHennaEmptySlots()
	{
		int totalSlots = 1 + getClassId().level();
		
		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] != null)
			{
				totalSlots--;
			}
		}
		
		if (totalSlots <= 0)
		{
			return 0;
		}
		
		return totalSlots;
	}
	
	/**
	 * Remove a Henna of the L2PcInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2PcInstance.
	 * @param slot the slot
	 * @return true, if successful
	 */
	public boolean removeHenna(int slot)
	{
		if ((slot < 1) || (slot > 3))
		{
			return false;
		}
		
		slot--;
		
		if (_henna[slot] == null)
		{
			return false;
		}
		
		L2HennaInstance henna = _henna[slot];
		_henna[slot] = null;
		
		java.sql.Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_CHAR_HENNA);
			statement.setInt(1, getObjectId());
			statement.setInt(2, slot + 1);
			statement.setInt(3, getClassIndex());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("could not remove char henna: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
		
		// Calculate Henna modifiers of this L2PcInstance
		recalcHennaStats();
		
		// Send Server->Client HennaInfo packet to this L2PcInstance
		sendPacket(new HennaInfo(this));
		
		// Send Server->Client UserInfo packet to this L2PcInstance
		sendPacket(new UserInfo(this));
		
		// Add the recovered dyes to the player's inventory and notify them.
		getInventory().addItem("Henna", henna.getItemIdDye(), henna.getAmountDyeRequire() / 2, this, null);
		
		SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
		sm.addItemName(henna.getItemIdDye());
		sm.addNumber(henna.getAmountDyeRequire() / 2);
		sendPacket(sm);
		
		return true;
	}
	
	/**
	 * Add a Henna to the L2PcInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2PcInstance.
	 * @param henna the henna
	 * @return true, if successful
	 */
	public boolean addHenna(L2HennaInstance henna)
	{
		if (getHennaEmptySlots() == 0)
		{
			sendMessage("You may not have more than three equipped symbols at a time.");
			return false;
		}
		
		// int slot = 0;
		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] == null)
			{
				_henna[i] = henna;
				
				// Calculate Henna modifiers of this L2PcInstance
				recalcHennaStats();
				
				java.sql.Connection con = null;
				
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement(ADD_CHAR_HENNA);
					statement.setInt(1, getObjectId());
					statement.setInt(2, henna.getSymbolId());
					statement.setInt(3, i + 1);
					statement.setInt(4, getClassIndex());
					statement.execute();
					statement.close();
				}
				catch (Exception e)
				{
					_log.warning("could not save char henna: " + e);
				}
				finally
				{
					try
					{
						con.close();
					}
					catch (Exception e)
					{
					}
				}
				
				// Send Server->Client HennaInfo packet to this L2PcInstance
				HennaInfo hi = new HennaInfo(this);
				sendPacket(hi);
				
				// Send Server->Client UserInfo packet to this L2PcInstance
				UserInfo ui = new UserInfo(this);
				sendPacket(ui);
				
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Calculate Henna modifiers of this L2PcInstance.
	 */
	private void recalcHennaStats()
	{
		_hennaINT = 0;
		_hennaSTR = 0;
		_hennaCON = 0;
		_hennaMEN = 0;
		_hennaWIT = 0;
		_hennaDEX = 0;
		
		for (int i = 0; i < 3; i++)
		{
			if (_henna[i] == null)
			{
				continue;
			}
			_hennaINT += _henna[i].getStatINT();
			_hennaSTR += _henna[i].getStatSTR();
			_hennaMEN += _henna[i].getStatMEM();
			_hennaCON += _henna[i].getStatCON();
			_hennaWIT += _henna[i].getStatWIT();
			_hennaDEX += _henna[i].getStatDEX();
		}
		
		if (_hennaINT > 5)
		{
			_hennaINT = 5;
		}
		if (_hennaSTR > 5)
		{
			_hennaSTR = 5;
		}
		if (_hennaMEN > 5)
		{
			_hennaMEN = 5;
		}
		if (_hennaCON > 5)
		{
			_hennaCON = 5;
		}
		if (_hennaWIT > 5)
		{
			_hennaWIT = 5;
		}
		if (_hennaDEX > 5)
		{
			_hennaDEX = 5;
		}
	}
	
	/**
	 * Return the Henna of this L2PcInstance corresponding to the selected slot.
	 * @param slot the slot
	 * @return the henna
	 */
	public L2HennaInstance getHenna(int slot)
	{
		if ((slot < 1) || (slot > 3))
		{
			return null;
		}
		
		return _henna[slot - 1];
	}
	
	/**
	 * Return the INT Henna modifier of this L2PcInstance.
	 * @return the henna stat int
	 */
	public int getHennaStatINT()
	{
		return _hennaINT;
	}
	
	/**
	 * Return the STR Henna modifier of this L2PcInstance.
	 * @return the henna stat str
	 */
	public int getHennaStatSTR()
	{
		return _hennaSTR;
	}
	
	/**
	 * Return the CON Henna modifier of this L2PcInstance.
	 * @return the henna stat con
	 */
	public int getHennaStatCON()
	{
		return _hennaCON;
	}
	
	/**
	 * Return the MEN Henna modifier of this L2PcInstance.
	 * @return the henna stat men
	 */
	public int getHennaStatMEN()
	{
		return _hennaMEN;
	}
	
	/**
	 * Return the WIT Henna modifier of this L2PcInstance.
	 * @return the henna stat wit
	 */
	public int getHennaStatWIT()
	{
		return _hennaWIT;
	}
	
	/**
	 * Return the DEX Henna modifier of this L2PcInstance.
	 * @return the henna stat dex
	 */
	public int getHennaStatDEX()
	{
		return _hennaDEX;
	}
	
	/**
	 * Return True if the L2PcInstance is autoAttackable. <br>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>Check if the attacker isn't the L2PcInstance Pet</li> <li>Check if the attacker is L2MonsterInstance</li> <li>If the attacker is a L2PcInstance, check if it is not in the same party</li> <li>Check if the L2PcInstance has Karma</li> <li>If the attacker is a L2PcInstance, check if it is not
	 * in the same siege clan (Attacker, Defender)</li><BR>
	 * @param attacker the attacker
	 * @return true, if is auto attackable
	 */
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		// Check if the attacker isn't the L2PcInstance Pet
		if ((attacker == this) || (attacker == getPet()))
		{
			return false;
		}
		
		// TODO: check for friendly mobs
		// Check if the attacker is a L2MonsterInstance
		if (attacker instanceof L2MonsterInstance)
		{
			return true;
		}
		
		// Check if the attacker is not in the same party
		if ((getParty() != null) && getParty().getPartyMembers().contains(attacker))
		{
			return false;
		}
		
		// Check if the attacker is in olympia and olympia start
		if ((attacker instanceof L2PcInstance) && ((L2PcInstance) attacker).isInOlympiadMode())
		{
			if (isInOlympiadMode() && isOlympiadStart() && (((L2PcInstance) attacker).getOlympiadGameId() == getOlympiadGameId()))
			{
				return true;
			}
			return false;
		}
		
		// Check if the attacker is not in the same clan
		if ((getClan() != null) && (attacker != null) && getClan().isMember(attacker.getName()))
		{
			return false;
		}
		
		if ((attacker instanceof L2PlayableInstance) && isInsideZone(ZONE_PEACE))
		{
			return false;
		}
		
		// Check if the L2PcInstance has Karma
		if ((getKarma() > 0) || (getPvpFlag() > 0))
		{
			return true;
		}
		
		// Check if the attacker is a L2PcInstance
		if (attacker instanceof L2PcInstance)
		{
			// is AutoAttackable if both players are in the same duel and the duel is still going on
			if ((getDuelState() == Duel.DUELSTATE_DUELLING) && (getDuelId() == ((L2PcInstance) attacker).getDuelId()))
			{
				return true;
			}
			// Check if the L2PcInstance is in an arena or a siege area
			if (isInsideZone(ZONE_PVP) && ((L2PcInstance) attacker).isInsideZone(ZONE_PVP))
			{
				return true;
			}
			
			if (getClan() != null)
			{
				Siege siege = SiegeManager.getInstance().getSiege(getX(), getY(), getZ());
				if (siege != null)
				{
					// Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Defender clan
					if (siege.checkIsDefender(((L2PcInstance) attacker).getClan()) && siege.checkIsDefender(getClan()))
					{
						return false;
					}
					
					// Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Attacker clan
					if (siege.checkIsAttacker(((L2PcInstance) attacker).getClan()) && siege.checkIsAttacker(getClan()))
					{
						return false;
					}
				}
				
				// Check if clan is at war
				if ((getClan() != null) && (((L2PcInstance) attacker).getClan() != null) && (getClan().isAtWarWith(((L2PcInstance) attacker).getClanId()) && (getWantsPeace() == 0) && (((L2PcInstance) attacker).getWantsPeace() == 0) && !isAcademyMember()))
				{
					return true;
				}
			}
		}
		else if (attacker instanceof L2SiegeGuardInstance)
		{
			if (getClan() != null)
			{
				Siege siege = SiegeManager.getInstance().getSiege(this);
				return ((siege != null) && siege.checkIsAttacker(getClan()));
			}
		}
		
		return false;
	}
	
	/**
	 * Check if the active L2Skill can be casted.<BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>Check if the skill isn't toggle and is offensive</li> <li>Check if the target is in the skill cast range</li> <li>Check if the skill is Spoil type and if the target isn't already spoiled</li> <li>Check if the caster owns enought consummed Item, enough HP and MP to cast the skill</li> <li>
	 * Check if the caster isn't sitting</li> <li>Check if all skills are enabled and this skill is enabled</li><BR>
	 * <li>Check if the caster own the weapon needed</li><BR>
	 * <li>Check if the skill is active</li><BR>
	 * <li>Check if all casting conditions are completed</li><BR>
	 * <li>Notify the AI with AI_INTENTION_CAST and target</li><BR>
	 * @param skill The L2Skill to use
	 * @param forceUse used to force ATTACK on players
	 * @param dontMove used to prevent movement, if not in range
	 */
	public void useMagic(L2Skill skill, boolean forceUse, boolean dontMove)
	{
		if (isDead())
		{
			abortCast();
			sendPacket(new ActionFailed());
			return;
		}
		
		/*
		 * if (isWearingFormalWear() && !skill.isPotion()) { sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_ITEMS_SKILLS_WITH_FORMALWEAR)); sendPacket(new ActionFailed()); abortCast(); return; }
		 */
		if (inObserverMode())
		{
			sendPacket(new SystemMessage(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE));
			abortCast();
			sendPacket(new ActionFailed());
			return;
		}
		
		// Check if the skill type is TOGGLE
		if (skill.isToggle())
		{
			// Get effects of the skill
			L2Effect effect = getFirstEffect(skill);
			
			if (effect != null)
			{
				effect.exit();
				
				// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(new ActionFailed());
				return;
			}
		}
		
		// Check if the skill is active
		if (skill.isPassive())
		{
			// just ignore the passive skill request. why does the client send it anyway ??
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(new ActionFailed());
			return;
		}
		
		// Check if it's ok to summon
		// siege golem (13), Wild Hog Cannon (299), Swoop Cannon (448)
		if (((skill.getId() == 13) || (skill.getId() == 299) || (skill.getId() == 448)) && !SiegeManager.getInstance().checkIfOkToSummon(this, false))
		{
			return;
		}
		
		// ************************************* Check Casting in Progress *******************************************
		
		// If a skill is currently being used, queue this one if this is not the same
		// Note that this check is currently imperfect: getCurrentSkill() isn't always null when a skill has
		// failed to cast, or the casting is not yet in progress when this is rechecked
		if ((getCurrentSkill() != null) && isCastingNow())
		{
			// Check if new skill different from current skill in progress
			if (skill.getId() == getCurrentSkill().getSkillId())
			{
				sendPacket(new ActionFailed());
				return;
			}
			
			if (Config.DEBUG && (getQueuedSkill() != null))
			{
				_log.info(getQueuedSkill().getSkill().getName() + " is already queued for " + getName() + ".");
			}
			
			// Create a new SkillDat object and queue it in the player _queuedSkill
			setQueuedSkill(skill, forceUse, dontMove);
			sendPacket(new ActionFailed());
			return;
		}
		if (getQueuedSkill() != null)
		{
			setQueuedSkill(null, false, false);
		}
		
		// ************************************* Check Target *******************************************
		
		// Create and set a L2Object containing the target of the skill
		L2Object target = null;
		SkillTargetType sklTargetType = skill.getTargetType();
		SkillType sklType = skill.getSkillType();
		
		switch (sklTargetType)
		{
		// Target the player if skill type is AURA, PARTY, CLAN or SELF
			case TARGET_AURA:
			case TARGET_PARTY:
			case TARGET_ALLY:
			case TARGET_CLAN:
			case TARGET_SELF:
				target = this;
				break;
			case TARGET_PET:
				target = getPet();
				break;
			default:
				target = getTarget();
				break;
		}
		
		// Check the validity of the target
		if (target == null)
		{
			sendPacket(new SystemMessage(SystemMessageId.TARGET_CANT_FOUND));
			sendPacket(new ActionFailed());
			return;
		}
		// Are the target and the player in the same duel?
		if (isInDuel())
		{
			if (!((target instanceof L2PcInstance) && (((L2PcInstance) target).getDuelId() == getDuelId())))
			{
				sendMessage("You cannot do this while duelling.");
				sendPacket(new ActionFailed());
				return;
			}
		}
		
		// ************************************* Check skill availability *******************************************
		
		// Check if this skill is enabled (ex : reuse time)
		if (isSkillDisabled(skill.getId()) && (getAccessLevel() < Config.GM_PEACEATTACK))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.SKILL_NOT_AVAILABLE);
			sm.addString(skill.getName());
			sendPacket(sm);
			
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(new ActionFailed());
			return;
		}
		
		// Check if all skills are disabled
		if (isAllSkillsDisabled() && (getAccessLevel() < Config.GM_PEACEATTACK))
		{
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(new ActionFailed());
			return;
		}
		
		// ************************************* Check Consumables *******************************************
		
		// Check if the caster has enough MP
		if (getCurrentMp() < (getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill)))
		{
			// Send a System Message to the caster
			sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_MP));
			
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(new ActionFailed());
			return;
		}
		
		// Check if the caster has enough HP
		if (getCurrentHp() <= skill.getHpConsume())
		{
			// Send a System Message to the caster
			sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_HP));
			
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(new ActionFailed());
			return;
		}
		
		// Check if the spell consummes an Item
		if (skill.getItemConsume() > 0)
		{
			// Get the L2ItemInstance consummed by the spell
			L2ItemInstance requiredItems = getInventory().getItemByItemId(skill.getItemConsumeId());
			
			// Check if the caster owns enought consummed Item to cast
			if ((requiredItems == null) || (requiredItems.getCount() < skill.getItemConsume()))
			{
				// Checked: when a summon skill failed, server show required consume item count
				if (sklType == L2Skill.SkillType.SUMMON)
				{
					SystemMessage sm = new SystemMessage(SystemMessageId.SUMMONING_SERVITOR_COSTS_S2_S1);
					sm.addItemName(skill.getItemConsumeId());
					sm.addNumber(skill.getItemConsume());
					sendPacket(sm);
				}
				else
				{
					// Send a System Message to the caster
					sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				}
				return;
			}
		}
		
		// ************************************* Check Casting Conditions *******************************************
		
		// Check if the caster own the weapon needed
		if (!skill.getWeaponDependancy(this))
		{
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(new ActionFailed());
			return;
		}
		
		// Check if all casting conditions are completed
		if (!skill.checkCondition(this, target, false))
		{
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(new ActionFailed());
			return;
		}
		
		// ************************************* Check Player State *******************************************
		
		// Abnormal effects(ex : Stun, Sleep...) are checked in L2Character useMagic()
		
		// Check if the player use "Fake Death" skill
		if (isAlikeDead())
		{
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(new ActionFailed());
			return;
		}
		
		// Check if the caster is sitting
		if (isSitting() && !skill.isPotion())
		{
			// Send a System Message to the caster
			sendPacket(new SystemMessage(SystemMessageId.CANT_MOVE_SITTING));
			
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(new ActionFailed());
			return;
		}
		
		if (isFishing() && ((sklType != SkillType.PUMPING) && (sklType != SkillType.REELING) && (sklType != SkillType.FISHING)))
		{
			// Only fishing skills are available
			sendPacket(new SystemMessage(SystemMessageId.ONLY_FISHING_SKILLS_NOW));
			return;
		}
		
		// ************************************* Check Skill Type *******************************************
		
		// Check if this is offensive magic skill
		if (skill.isOffensive())
		{
			if ((isInsidePeaceZone(this, target)) && (getAccessLevel() < Config.GM_PEACEATTACK))
			{
				// If L2Character or target is in a peace zone, send a system message TARGET_IN_PEACEZONE a Server->Client packet ActionFailed
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
				sendPacket(new ActionFailed());
				return;
			}
			
			if (isInOlympiadMode() && !isOlympiadStart())
			{
				// if L2PcInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
				sendPacket(new ActionFailed());
				return;
			}
			
			// Check if the target is attackable
			if (!target.isAttackable() && (getAccessLevel() < Config.GM_PEACEATTACK))
			{
				// If target is not attackable, send a Server->Client packet ActionFailed
				sendPacket(new ActionFailed());
				return;
			}
			
			// Check if a Forced ATTACK is in progress on non-attackable target
			if (!target.isAutoAttackable(this) && !forceUse && (sklTargetType != SkillTargetType.TARGET_AURA) && (sklTargetType != SkillTargetType.TARGET_CLAN) && (sklTargetType != SkillTargetType.TARGET_ALLY) && (sklTargetType != SkillTargetType.TARGET_PARTY) && (sklTargetType != SkillTargetType.TARGET_SELF))
			{
				// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(new ActionFailed());
				return;
			}
			
			// Check if the target is in the skill cast range
			if (dontMove)
			{
				// Calculate the distance between the L2PcInstance and the target
				if ((skill.getCastRange() > 0) && !isInsideRadius(target, skill.getCastRange() + getTemplate().collisionRadius, false, false))
				{
					// Send a System Message to the caster
					sendPacket(new SystemMessage(SystemMessageId.TARGET_TOO_FAR));
					
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(new ActionFailed());
					return;
				}
			}
		}
		
		// Check if the skill is defensive
		if (!skill.isOffensive())
		{
			// check if the target is a monster and if force attack is set.. if not then we don't want to cast.
			if ((target instanceof L2MonsterInstance) && !forceUse && (sklTargetType != SkillTargetType.TARGET_PET) && (sklTargetType != SkillTargetType.TARGET_AURA) && (sklTargetType != SkillTargetType.TARGET_CLAN) && (sklTargetType != SkillTargetType.TARGET_SELF) && (sklTargetType != SkillTargetType.TARGET_PARTY) && (sklTargetType != SkillTargetType.TARGET_ALLY) && (sklTargetType != SkillTargetType.TARGET_CORPSE_MOB) && (sklTargetType != SkillTargetType.TARGET_AREA_CORPSE_MOB) && (sklType != SkillType.BEAST_FEED) && (sklType != SkillType.DELUXE_KEY_UNLOCK) && (sklType != SkillType.UNLOCK))
			{
				// send the action failed so that the skill doens't go off.
				sendPacket(new ActionFailed());
				return;
			}
		}
		
		// Check if the skill is Spoil type and if the target isn't already spoiled
		if (sklType == SkillType.SPOIL)
		{
			if (!(target instanceof L2MonsterInstance))
			{
				// Send a System Message to the L2PcInstance
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				
				// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(new ActionFailed());
				return;
			}
		}
		
		// Check if the skill is Sweep type and if conditions not apply
		if ((sklType == SkillType.SWEEP) && (target instanceof L2Attackable))
		{
			int spoilerId = ((L2Attackable) target).getIsSpoiledBy();
			
			if (((L2Attackable) target).isDead())
			{
				if (!((L2Attackable) target).isSpoil())
				{
					// Send a System Message to the L2PcInstance
					sendPacket(new SystemMessage(SystemMessageId.SWEEPER_FAILED_TARGET_NOT_SPOILED));
					
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(new ActionFailed());
					return;
				}
				
				if ((getObjectId() != spoilerId) && !isInLooterParty(spoilerId))
				{
					// Send a System Message to the L2PcInstance
					sendPacket(new SystemMessage(SystemMessageId.SWEEP_NOT_ALLOWED));
					
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(new ActionFailed());
					return;
				}
			}
		}
		
		// Check if the skill is Drain Soul (Soul Crystals) and if the target is a MOB
		if (sklType == SkillType.DRAIN_SOUL)
		{
			if (!(target instanceof L2MonsterInstance))
			{
				// Send a System Message to the L2PcInstance
				sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
				
				// Send a Server->Client packet ActionFailed to the L2PcInstance
				sendPacket(new ActionFailed());
				return;
			}
		}
		
		// Check if this is a Pvp skill and target isn't a non-flagged/non-karma player
		switch (sklTargetType)
		{
			case TARGET_PARTY:
			case TARGET_ALLY: // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
			case TARGET_CLAN: // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
			case TARGET_AURA:
			case TARGET_SELF:
				break;
			default:
				if (!checkPvpSkill(target, skill) && (getAccessLevel() < Config.GM_PEACEATTACK))
				{
					// Send a System Message to the L2PcInstance
					sendPacket(new SystemMessage(SystemMessageId.TARGET_IS_INCORRECT));
					
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					sendPacket(new ActionFailed());
					return;
				}
		}
		
		if ((sklTargetType == SkillTargetType.TARGET_HOLY) && !TakeCastle.checkIfOkToCastSealOfRule(this, false))
		{
			sendPacket(new ActionFailed());
			abortCast();
			return;
		}
		
		if ((sklType == SkillType.SIEGEFLAG) && !SiegeFlag.checkIfOkToPlaceFlag(this, false))
		{
			sendPacket(new ActionFailed());
			abortCast();
			return;
		}
		else if ((sklType == SkillType.STRSIEGEASSAULT) && !StrSiegeAssault.checkIfOkToUseStriderSiegeAssault(this, false))
		{
			sendPacket(new ActionFailed());
			abortCast();
			return;
		}
		
		// GeoData Los Check here
		if ((skill.getCastRange() > 0) && !GeoData.getInstance().canSeeTarget(this, target))
		{
			sendPacket(new SystemMessage(SystemMessageId.CANT_SEE_TARGET));
			sendPacket(new ActionFailed());
			return;
		}
		
		// If all conditions are checked, create a new SkillDat object and set the player _currentSkill
		setCurrentSkill(skill, forceUse, dontMove);
		
		// Check if the active L2Skill can be casted (ex : not sleeping...), Check if the target is correct and Notify the AI with AI_INTENTION_CAST and target
		super.useMagic(skill);
		
	}
	
	/**
	 * Checks if is in looter party.
	 * @param LooterId the looter id
	 * @return true, if is in looter party
	 */
	public boolean isInLooterParty(int LooterId)
	{
		L2PcInstance looter = (L2PcInstance) L2World.getInstance().findObject(LooterId);
		
		// if L2PcInstance is in a CommandChannel
		if (isInParty() && getParty().isInCommandChannel() && (looter != null))
		{
			return getParty().getCommandChannel().getMembers().contains(looter);
		}
		
		if (isInParty() && (looter != null))
		{
			return getParty().getPartyMembers().contains(looter);
		}
		
		return false;
	}
	
	/**
	 * Check if the requested casting is a Pc->Pc skill cast and if it's a valid pvp condition.
	 * @param target L2Object instance containing the target
	 * @param skill L2Skill instance with the skill being casted
	 * @return False if the skill is a pvpSkill and target is not a valid pvp target
	 */
	public boolean checkPvpSkill(L2Object target, L2Skill skill)
	{
		// check for PC->PC Pvp status
		if ((target != null) && // target not null and
		(target != this) && // target is not self and
		(target instanceof L2PcInstance) && // target is L2PcInstance and
		!(isInDuel() && (((L2PcInstance) target).getDuelId() == getDuelId())) && // self is not in a duel and attacking opponent
		!isInsideZone(ZONE_PVP) && // Pc is not in PvP zone
		!((L2PcInstance) target).isInsideZone(ZONE_PVP) // target is not in PvP zone
		)
		{
			if (skill.isPvpSkill()) // pvp skill
			{
				if ((getClan() != null) && (((L2PcInstance) target).getClan() != null))
				{
					if (getClan().isAtWarWith(((L2PcInstance) target).getClan().getClanId()))
					{
						return true; // in clan war player can attack whites even with sleep etc.
					}
				}
				if ((((L2PcInstance) target).getPvpFlag() == 0) && // target's pvp flag is not set and
				(((L2PcInstance) target).getKarma() == 0 // target has no karma
				))
				{
					return false;
				}
			}
			else if ((getCurrentSkill() != null) && !getCurrentSkill().isCtrlPressed() && skill.isOffensive())
			{
				if ((((L2PcInstance) target).getPvpFlag() == 0) && // target's pvp flag is not set and
				(((L2PcInstance) target).getKarma() == 0 // target has no karma
				))
				{
					return false;
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Reduce Item quantity of the L2PcInstance Inventory and send it a Server->Client packet InventoryUpdate.
	 * @param itemConsumeId the item consume id
	 * @param itemCount the item count
	 */
	@Override
	public void consumeItem(int itemConsumeId, int itemCount)
	{
		if ((itemConsumeId != 0) && (itemCount != 0))
		{
			destroyItemByItemId("Consume", itemConsumeId, itemCount, null, false);
		}
	}
	
	/**
	 * Return True if the L2PcInstance is a Mage.
	 * @return true, if is mage class
	 */
	public boolean isMageClass()
	{
		return getClassId().isMage();
	}
	
	/**
	 * Checks if is mounted.
	 * @return true, if is mounted
	 */
	public boolean isMounted()
	{
		return _mountType > 0;
	}
	
	/**
	 * Set the type of Pet mounted (0 : none, 1 : Stridder, 2 : Wyvern) and send a Server->Client packet InventoryUpdate to the L2PcInstance.
	 * @return true, if successful
	 */
	public boolean checkLandingState()
	{
		// Check if char is in a no landing zone
		if (isInsideZone(ZONE_NOLANDING))
		{
			return true;
		}
		else
		// if this is a castle that is currently being sieged, and the rider is NOT a castle owner
		// he cannot land.
		// castle owner is the leader of the clan that owns the castle where the pc is
		if (isInsideZone(ZONE_SIEGE) && !((getClan() != null) && (CastleManager.getInstance().getCastle(this) == CastleManager.getInstance().getCastleByOwner(getClan())) && (this == getClan().getLeader().getPlayerInstance())))
		{
			return true;
		}
		
		return false;
	}
	
	// returns false if the change of mount type fails.
	/**
	 * Sets the mount type.
	 * @param mountType the mount type
	 * @return true, if successful
	 */
	public boolean setMountType(int mountType)
	{
		if (checkLandingState() && (mountType == 2))
		{
			return false;
		}
		
		switch (mountType)
		{
			case 0:
				setIsFlying(false);
				setIsRiding(false);
				break; // Dismounted
			case 1:
				setIsRiding(true);
				if (isNoble())
				{
					L2Skill striderAssaultSkill = SkillTable.getInstance().getInfo(325, 1);
					addSkill(striderAssaultSkill, false); // not saved to DB
				}
				break;
			case 2:
				setIsFlying(true);
				break; // Flying Wyvern
		}
		
		_mountType = mountType;
		
		// Send a Server->Client packet InventoryUpdate to the L2PcInstance in order to update speed
		UserInfo ui = new UserInfo(this);
		sendPacket(ui);
		return true;
	}
	
	/**
	 * Return the type of Pet mounted (0 : none, 1 : Stridder, 2 : Wyvern).
	 * @return the mount type
	 */
	public int getMountType()
	{
		return _mountType;
	}
	
	/**
	 * Send a Server->Client packet UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers.<BR>
	 * <B><U> Concept</U> :</B><BR>
	 * Others L2PcInstance in the detection area of the L2PcInstance are identified in <B>_knownPlayers</B>. In order to inform other players of this L2PcInstance state modifications, server just need to go through _knownPlayers to send Server->Client Packet<BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>Send a Server->Client packet UserInfo to this L2PcInstance (Public and Private Data)</li> <li>Send a Server->Client packet CharInfo to all L2PcInstance in _KnownPlayers of the L2PcInstance (Public data only)</li><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : DON'T SEND UserInfo packet to other players instead of CharInfo packet. Indeed, UserInfo packet contains PRIVATE DATA as MaxHP, STR, DEX...</B></FONT><BR>
	 */
	@Override
	public void updateAbnormalEffect()
	{
		broadcastUserInfo();
	}
	
	/**
	 * Disable the Inventory and create a new task to enable it after 1.5s.
	 */
	public void tempInvetoryDisable()
	{
		_inventoryDisable = true;
		
		ThreadPoolManager.getInstance().scheduleGeneral(new InventoryEnable(), 1500);
	}
	
	/**
	 * Return True if the Inventory is disabled.
	 * @return true, if is invetory disabled
	 */
	public boolean isInvetoryDisabled()
	{
		return _inventoryDisable;
	}
	
	/**
	 * The Class InventoryEnable.
	 */
	class InventoryEnable implements Runnable
	{
		@Override
		public void run()
		{
			_inventoryDisable = false;
		}
	}
	
	/**
	 * Gets the cubics.
	 * @return the cubics
	 */
	public Map<Integer, L2CubicInstance> getCubics()
	{
		return _cubics;
	}
	
	/**
	 * Add a L2CubicInstance to the L2PcInstance _cubics.
	 * @param id the id
	 * @param level the level
	 */
	public void addCubic(int id, int level)
	{
		L2CubicInstance cubic = new L2CubicInstance(this, id, level);
		_cubics.put(id, cubic);
	}
	
	/**
	 * Remove a L2CubicInstance from the L2PcInstance _cubics.
	 * @param id the id
	 */
	public void delCubic(int id)
	{
		_cubics.remove(id);
	}
	
	/**
	 * Return the L2CubicInstance corresponding to the Identifier of the L2PcInstance _cubics.
	 * @param id the id
	 * @return the cubic
	 */
	public L2CubicInstance getCubic(int id)
	{
		return _cubics.get(id);
	}
	
	@Override
	public String toString()
	{
		return "player " + getName();
	}
	
	/**
	 * Return the modifier corresponding to the Enchant Effect of the Active Weapon (Min : 127).
	 * @return the enchant effect
	 */
	public int getEnchantEffect()
	{
		L2ItemInstance wpn = getActiveWeaponInstance();
		
		if (wpn == null)
		{
			return 0;
		}
		
		return Math.min(127, wpn.getEnchantLevel());
	}
	
	/**
	 * Set the _lastFolkNpc of the L2PcInstance corresponding to the last Folk wich one the player talked.
	 * @param folkNpc the new last folk npc
	 */
	public void setLastFolkNPC(L2FolkInstance folkNpc)
	{
		_lastFolkNpc = folkNpc;
	}
	
	/**
	 * Return the _lastFolkNpc of the L2PcInstance corresponding to the last Folk wich one the player talked.
	 * @return the last folk npc
	 */
	public L2FolkInstance getLastFolkNPC()
	{
		return _lastFolkNpc;
	}
	
	/**
	 * Set the Silent Moving mode Flag.
	 * @param flag the new silent moving
	 */
	public void setSilentMoving(boolean flag)
	{
		_isSilentMoving = flag;
	}
	
	/**
	 * Return True if the Silent Moving mode is active.
	 * @return true, if is silent moving
	 */
	public boolean isSilentMoving()
	{
		return _isSilentMoving;
	}
	
	/**
	 * Return True if L2PcInstance is a participant in the Festival of Darkness.
	 * @return true, if is festival participant
	 */
	public boolean isFestivalParticipant()
	{
		return SevenSignsFestival.getInstance().isParticipant(this);
	}
	
	/**
	 * Adds the auto soul shot.
	 * @param itemId the item id
	 */
	public void addAutoSoulShot(int itemId)
	{
		_activeSoulShots.put(itemId, itemId);
	}
	
	/**
	 * Removes the auto soul shot.
	 * @param itemId the item id
	 */
	public void removeAutoSoulShot(int itemId)
	{
		_activeSoulShots.remove(itemId);
	}
	
	/**
	 * Gets the auto soul shot.
	 * @return the auto soul shot
	 */
	public Map<Integer, Integer> getAutoSoulShot()
	{
		return _activeSoulShots;
	}
	
	/**
	 * Recharge auto soul shot.
	 * @param physical the physical
	 * @param magic the magic
	 * @param summon the summon
	 */
	public void rechargeAutoSoulShot(boolean physical, boolean magic, boolean summon)
	{
		L2ItemInstance item;
		IItemHandler handler;
		
		if ((_activeSoulShots == null) || (_activeSoulShots.size() == 0))
		{
			return;
		}
		
		for (int itemId : _activeSoulShots.values())
		{
			item = getInventory().getItemByItemId(itemId);
			
			if (item != null)
			{
				if (magic)
				{
					if (!summon)
					{
						if ((itemId == 2509) || (itemId == 2510) || (itemId == 2511) || (itemId == 2512) || (itemId == 2513) || (itemId == 2514) || (itemId == 3947) || (itemId == 3948) || (itemId == 3949) || (itemId == 3950) || (itemId == 3951) || (itemId == 3952) || (itemId == 5790))
						{
							handler = ItemHandler.getInstance().getItemHandler(itemId);
							
							if (handler != null)
							{
								handler.useItem(this, item);
							}
						}
					}
					else
					{
						if ((itemId == 6646) || (itemId == 6647))
						{
							handler = ItemHandler.getInstance().getItemHandler(itemId);
							
							if (handler != null)
							{
								handler.useItem(this, item);
							}
						}
					}
				}
				
				if (physical)
				{
					if (!summon)
					{
						if ((itemId == 1463) || (itemId == 1464) || (itemId == 1465) || (itemId == 1466) || (itemId == 1467) || (itemId == 1835) || (itemId == 5789 /*
																																									 * || itemId == 6535 || itemId == 6536 || itemId == 6537 || itemId == 6538 || itemId == 6539 || itemId == 6540
																																									 */))
						{
							handler = ItemHandler.getInstance().getItemHandler(itemId);
							
							if (handler != null)
							{
								handler.useItem(this, item);
							}
						}
					}
					else
					{
						if (itemId == 6645)
						{
							handler = ItemHandler.getInstance().getItemHandler(itemId);
							
							if (handler != null)
							{
								handler.useItem(this, item);
							}
						}
					}
				}
			}
			else
			{
				removeAutoSoulShot(itemId);
			}
		}
	}
	
	/** The _task warn user take break. */
	private ScheduledFuture<?> _taskWarnUserTakeBreak;
	
	/**
	 * The Class WarnUserTakeBreak.
	 */
	class WarnUserTakeBreak implements Runnable
	{
		@Override
		public void run()
		{
			if (isOnline() == 1)
			{
				SystemMessage msg = new SystemMessage(SystemMessageId.PLAYING_FOR_LONG_TIME);
				sendPacket(msg);
			}
			else
			{
				stopWarnUserTakeBreak();
			}
		}
	}
	
	/**
	 * The Class RentPetTask.
	 */
	class RentPetTask implements Runnable
	{
		@Override
		public void run()
		{
			stopRentPet();
		}
	}
	
	/** The _taskforfish. */
	public ScheduledFuture<?> _taskforfish;
	
	/**
	 * The Class WaterTask.
	 */
	class WaterTask implements Runnable
	{
		@Override
		public void run()
		{
			double reduceHp = getMaxHp() / 100.0;
			if (reduceHp < 1)
			{
				reduceHp = 1;
			}
			
			reduceCurrentHp(reduceHp, L2PcInstance.this, false);
			// reduced hp, becouse not rest
			SystemMessage sm = new SystemMessage(SystemMessageId.DROWN_DAMAGE_S1);
			sm.addNumber((int) reduceHp);
			sendPacket(sm);
		}
	}
	
	/**
	 * The Class LookingForFishTask.
	 */
	class LookingForFishTask implements Runnable
	{
		/** The _is upper grade. */
		boolean _isNoob, _isUpperGrade;
		
		/** The _guts check time. */
		int _fishType, _fishGutsCheck, _gutsCheckTime;
		
		/** The _end task time. */
		long _endTaskTime;
		
		/**
		 * Instantiates a new looking for fish task.
		 * @param fishWaitTime the fish wait time
		 * @param fishGutsCheck the fish guts check
		 * @param fishType the fish type
		 * @param isNoob the is noob
		 * @param isUpperGrade the is upper grade
		 */
		protected LookingForFishTask(int fishWaitTime, int fishGutsCheck, int fishType, boolean isNoob, boolean isUpperGrade)
		{
			_fishGutsCheck = fishGutsCheck;
			_endTaskTime = System.currentTimeMillis() + fishWaitTime + 10000;
			_fishType = fishType;
			_isNoob = isNoob;
			_isUpperGrade = isUpperGrade;
		}
		
		@Override
		public void run()
		{
			if (System.currentTimeMillis() >= _endTaskTime)
			{
				EndFishing(false);
				return;
			}
			if (_fishType == -1)
			{
				return;
			}
			int check = Rnd.get(1000);
			if (_fishGutsCheck > check)
			{
				stopLookingForFishTask();
				StartFishCombat(_isNoob, _isUpperGrade);
			}
		}
	}
	
	/**
	 * Gets the clan privileges.
	 * @return the clan privileges
	 */
	public int getClanPrivileges()
	{
		return _clanPrivileges;
	}
	
	/**
	 * Sets the clan privileges.
	 * @param n the new clan privileges
	 */
	public void setClanPrivileges(int n)
	{
		_clanPrivileges = n;
	}
	
	// baron etc
	/**
	 * Sets the pledge class.
	 * @param classId the new pledge class
	 */
	public void setPledgeClass(int classId)
	{
		_pledgeClass = classId;
	}
	
	/**
	 * Gets the pledge class.
	 * @return the pledge class
	 */
	public int getPledgeClass()
	{
		return _pledgeClass;
	}
	
	/**
	 * Sets the pledge type.
	 * @param typeId the new pledge type
	 */
	public void setPledgeType(int typeId)
	{
		_pledgeType = typeId;
	}
	
	/**
	 * Gets the pledge type.
	 * @return the pledge type
	 */
	public int getPledgeType()
	{
		return _pledgeType;
	}
	
	/**
	 * Gets the apprentice.
	 * @return the apprentice
	 */
	public int getApprentice()
	{
		return _apprentice;
	}
	
	/**
	 * Sets the apprentice.
	 * @param apprentice_id the new apprentice
	 */
	public void setApprentice(int apprentice_id)
	{
		_apprentice = apprentice_id;
	}
	
	/**
	 * Gets the sponsor.
	 * @return the sponsor
	 */
	public int getSponsor()
	{
		return _sponsor;
	}
	
	/**
	 * Sets the sponsor.
	 * @param sponsor_id the new sponsor
	 */
	public void setSponsor(int sponsor_id)
	{
		_sponsor = sponsor_id;
	}
	
	/**
	 * Send message.
	 * @param message the message
	 */
	public void sendMessage(String message)
	{
		sendPacket(SystemMessage.sendString(message));
	}
	
	/**
	 * Enter observer mode.
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 */
	public void enterObserverMode(int x, int y, int z)
	{
		_obsX = getX();
		_obsY = getY();
		_obsZ = getZ();
		
		setTarget(null);
		stopMove(null);
		setIsParalyzed(true);
		setIsInvul(true);
		getAppearance().setInvisible();
		sendPacket(new ObservationMode(x, y, z));
		setXYZ(x, y, z);
		
		_observerMode = true;
		broadcastUserInfo();
	}
	
	/**
	 * Enter olympiad observer mode.
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 * @param id the id
	 */
	public void enterOlympiadObserverMode(int x, int y, int z, int id)
	{
		if (getPet() != null)
		{
			getPet().unSummon(this);
		}
		
		if (getCubics().size() > 0)
		{
			for (L2CubicInstance cubic : getCubics().values())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}
			
			getCubics().clear();
		}
		
		_olympiadGameId = id;
		_obsX = getX();
		if (isSitting())
		{
			standUp();
		}
		_obsY = getY();
		_obsZ = getZ();
		setTarget(null);
		setIsInvul(true);
		getAppearance().setInvisible();
		teleToLocation(x, y, z, true);
		sendPacket(new ExOlympiadMode(3));
		_observerMode = true;
		broadcastUserInfo();
	}
	
	/**
	 * Leave observer mode.
	 */
	public void leaveObserverMode()
	{
		setTarget(null);
		setXYZ(_obsX, _obsY, _obsZ);
		setIsParalyzed(false);
		getAppearance().setVisible();
		setIsInvul(false);
		
		if (getAI() != null)
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
		
		_observerMode = false;
		sendPacket(new ObservationReturn(this));
		broadcastUserInfo();
	}
	
	/**
	 * Leave olympiad observer mode.
	 */
	public void leaveOlympiadObserverMode()
	{
		setTarget(null);
		sendPacket(new ExOlympiadMode(0));
		teleToLocation(_obsX, _obsY, _obsZ, true);
		getAppearance().setVisible();
		setIsInvul(false);
		if (getAI() != null)
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		}
		Olympiad.getInstance().removeSpectator(_olympiadGameId, this);
		_olympiadGameId = -1;
		_observerMode = false;
		broadcastUserInfo();
	}
	
	/**
	 * Sets the olympiad side.
	 * @param i the new olympiad side
	 */
	public void setOlympiadSide(int i)
	{
		_olympiadSide = i;
	}
	
	/**
	 * Gets the olympiad side.
	 * @return the olympiad side
	 */
	public int getOlympiadSide()
	{
		return _olympiadSide;
	}
	
	/**
	 * Sets the olympiad game id.
	 * @param id the new olympiad game id
	 */
	public void setOlympiadGameId(int id)
	{
		_olympiadGameId = id;
	}
	
	/**
	 * Gets the olympiad game id.
	 * @return the olympiad game id
	 */
	public int getOlympiadGameId()
	{
		return _olympiadGameId;
	}
	
	/**
	 * Gets the obs x.
	 * @return the obs x
	 */
	public int getObsX()
	{
		return _obsX;
	}
	
	/**
	 * Gets the obs y.
	 * @return the obs y
	 */
	public int getObsY()
	{
		return _obsY;
	}
	
	/**
	 * Gets the obs z.
	 * @return the obs z
	 */
	public int getObsZ()
	{
		return _obsZ;
	}
	
	/**
	 * In observer mode.
	 * @return true, if successful
	 */
	public boolean inObserverMode()
	{
		return _observerMode;
	}
	
	/**
	 * Gets the tele mode.
	 * @return the tele mode
	 */
	public int getTeleMode()
	{
		return _telemode;
	}
	
	/**
	 * Sets the tele mode.
	 * @param mode the new tele mode
	 */
	public void setTeleMode(int mode)
	{
		_telemode = mode;
	}
	
	/**
	 * Sets the loto.
	 * @param i the i
	 * @param val the val
	 */
	public void setLoto(int i, int val)
	{
		_loto[i] = val;
	}
	
	/**
	 * Gets the loto.
	 * @param i the i
	 * @return the loto
	 */
	public int getLoto(int i)
	{
		return _loto[i];
	}
	
	/**
	 * Sets the race.
	 * @param i the i
	 * @param val the val
	 */
	public void setRace(int i, int val)
	{
		_race[i] = val;
	}
	
	/**
	 * Gets the race.
	 * @param i the i
	 * @return the race
	 */
	public int getRace(int i)
	{
		return _race[i];
	}
	
	/**
	 * Sets the chat banned.
	 * @param isBanned the new chat banned
	 */
	public void setChatBanned(boolean isBanned)
	{
		_chatBanned = isBanned;
		
		if (isChatBanned())
		{
			sendMessage("You have been chat banned by a server admin.");
		}
		else
		{
			sendMessage("Your chat ban has been lifted.");
			if (_chatUnbanTask != null)
			{
				_chatUnbanTask.cancel(false);
			}
			_chatUnbanTask = null;
		}
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * Checks if is chat banned.
	 * @return true, if is chat banned
	 */
	public boolean isChatBanned()
	{
		return _chatBanned;
	}
	
	/**
	 * Sets the chat unban task.
	 * @param task the new chat unban task
	 */
	public void setChatUnbanTask(ScheduledFuture<?> task)
	{
		_chatUnbanTask = task;
	}
	
	/**
	 * Gets the chat unban task.
	 * @return the chat unban task
	 */
	public ScheduledFuture<?> getChatUnbanTask()
	{
		return _chatUnbanTask;
	}
	
	/**
	 * Gets the message refusal.
	 * @return the message refusal
	 */
	public boolean getMessageRefusal()
	{
		return _messageRefusal;
	}
	
	/**
	 * Sets the message refusal.
	 * @param mode the new message refusal
	 */
	public void setMessageRefusal(boolean mode)
	{
		_messageRefusal = mode;
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * Sets the diet mode.
	 * @param mode the new diet mode
	 */
	public void setDietMode(boolean mode)
	{
		_dietMode = mode;
	}
	
	/**
	 * Gets the diet mode.
	 * @return the diet mode
	 */
	public boolean getDietMode()
	{
		return _dietMode;
	}
	
	/**
	 * Sets the trade refusal.
	 * @param mode the new trade refusal
	 */
	public void setTradeRefusal(boolean mode)
	{
		_tradeRefusal = mode;
	}
	
	/**
	 * Gets the trade refusal.
	 * @return the trade refusal
	 */
	public boolean getTradeRefusal()
	{
		return _tradeRefusal;
	}
	
	/**
	 * Sets the exchange refusal.
	 * @param mode the new exchange refusal
	 */
	public void setExchangeRefusal(boolean mode)
	{
		_exchangeRefusal = mode;
	}
	
	/**
	 * Gets the exchange refusal.
	 * @return the exchange refusal
	 */
	public boolean getExchangeRefusal()
	{
		return _exchangeRefusal;
	}
	
	/**
	 * Gets the block list.
	 * @return the block list
	 */
	public BlockList getBlockList()
	{
		return _blockList;
	}
	
	/**
	 * Sets the hero.
	 * @param hero the new hero
	 */
	public void setHero(boolean hero)
	{
		if (hero && (_baseClass == _activeClass))
		{
			for (L2Skill s : HeroSkillTable.GetHeroSkills())
			{
				addSkill(s, false); // Dont Save Hero skills to database
			}
		}
		else
		{
			for (L2Skill s : HeroSkillTable.GetHeroSkills())
			{
				super.removeSkill(s); // Just Remove skills from nonHero characters
			}
		}
		_hero = hero;
		
		sendSkillList();
	}
	
	/**
	 * Sets the checks if is in olympiad mode.
	 * @param b the new checks if is in olympiad mode
	 */
	public void setIsInOlympiadMode(boolean b)
	{
		_inOlympiadMode = b;
	}
	
	/**
	 * Sets the checks if is olympiad start.
	 * @param b the new checks if is olympiad start
	 */
	public void setIsOlympiadStart(boolean b)
	{
		_OlympiadStart = b;
	}
	
	/**
	 * Checks if is olympiad start.
	 * @return true, if is olympiad start
	 */
	public boolean isOlympiadStart()
	{
		return _OlympiadStart;
	}
	
	/**
	 * Checks if is hero.
	 * @return true, if is hero
	 */
	public boolean isHero()
	{
		return _hero;
	}
	
	/**
	 * Checks if is in olympiad mode.
	 * @return true, if is in olympiad mode
	 */
	public boolean isInOlympiadMode()
	{
		return _inOlympiadMode;
	}
	
	/**
	 * Checks if is in duel.
	 * @return true, if is in duel
	 */
	public boolean isInDuel()
	{
		return _isInDuel;
	}
	
	/**
	 * Gets the duel id.
	 * @return the duel id
	 */
	public int getDuelId()
	{
		return _duelId;
	}
	
	/**
	 * Sets the duel state.
	 * @param mode the new duel state
	 */
	public void setDuelState(int mode)
	{
		_duelState = mode;
	}
	
	/**
	 * Gets the duel state.
	 * @return the duel state
	 */
	public int getDuelState()
	{
		return _duelState;
	}
	
	/**
	 * Sets up the duel state using a non 0 duelId.
	 * @param duelId 0=not in a duel
	 */
	public void setIsInDuel(int duelId)
	{
		if (duelId > 0)
		{
			_isInDuel = true;
			_duelState = Duel.DUELSTATE_DUELLING;
			_duelId = duelId;
		}
		else
		{
			if (_duelState == Duel.DUELSTATE_DEAD)
			{
				enableAllSkills();
				getStatus().startHpMpRegeneration();
			}
			_isInDuel = false;
			_duelState = Duel.DUELSTATE_NODUEL;
			_duelId = 0;
		}
	}
	
	/**
	 * This returns a SystemMessage stating why the player is not available for duelling.
	 * @return S1_CANNOT_DUEL... message
	 */
	public SystemMessage getNoDuelReason()
	{
		SystemMessage sm = new SystemMessage(_noDuelReason);
		sm.addString(getName());
		_noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
		return sm;
	}
	
	/**
	 * Checks if this player might join / start a duel. To get the reason use getNoDuelReason() after calling this function.
	 * @return true if the player might join/start a duel.
	 */
	public boolean canDuel()
	{
		if (isInCombat() || isInJail())
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_BATTLE;
			return false;
		}
		if (isDead() || isAlikeDead() || ((getCurrentHp() < (getMaxHp() / 2)) || (getCurrentMp() < (getMaxMp() / 2))))
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1S_HP_OR_MP_IS_BELOW_50_PERCENT;
			return false;
		}
		if (isInDuel())
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_ALREADY_ENGAGED_IN_A_DUEL;
			return false;
		}
		if (isInOlympiadMode())
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_PARTICIPATING_IN_THE_OLYMPIAD;
			return false;
		}
		if (isCursedWeaponEquiped())
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_IN_A_CHAOTIC_STATE;
			return false;
		}
		if (getPrivateStoreType() != STORE_PRIVATE_NONE)
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_A_PRIVATE_STORE_OR_MANUFACTURE;
			return false;
		}
		if (isMounted() || isInBoat())
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_RIDING_A_BOAT_WYVERN_OR_STRIDER;
			return false;
		}
		if (isFishing())
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_FISHING;
			return false;
		}
		if (isInsideZone(ZONE_PVP) || isInsideZone(ZONE_PEACE) || isInsideZone(ZONE_SIEGE))
		{
			_noDuelReason = SystemMessageId.S1_CANNOT_MAKE_A_CHALLANGE_TO_A_DUEL_BECAUSE_S1_IS_CURRENTLY_IN_A_DUEL_PROHIBITED_AREA;
			return false;
		}
		return true;
	}
	
	/**
	 * Checks if is noble.
	 * @return true, if is noble
	 */
	public boolean isNoble()
	{
		return _noble;
	}
	
	/**
	 * Sets the noble.
	 * @param val the new noble
	 */
	public void setNoble(boolean val)
	{
		if (val)
		{
			for (L2Skill s : NobleSkillTable.getInstance().GetNobleSkills())
			{
				addSkill(s, false); // Dont Save Noble skills to Sql
			}
		}
		else
		{
			for (L2Skill s : NobleSkillTable.getInstance().GetNobleSkills())
			{
				super.removeSkill(s); // Just Remove skills without deleting from Sql
			}
		}
		_noble = val;
		
		sendSkillList();
	}
	
	/**
	 * Sets the lvl joined academy.
	 * @param lvl the new lvl joined academy
	 */
	public void setLvlJoinedAcademy(int lvl)
	{
		_lvlJoinedAcademy = lvl;
	}
	
	/**
	 * Gets the lvl joined academy.
	 * @return the lvl joined academy
	 */
	public int getLvlJoinedAcademy()
	{
		return _lvlJoinedAcademy;
	}
	
	/**
	 * Checks if is academy member.
	 * @return true, if is academy member
	 */
	public boolean isAcademyMember()
	{
		return _lvlJoinedAcademy > 0;
	}
	
	/**
	 * Sets the team.
	 * @param team the new team
	 */
	public void setTeam(int team)
	{
		_team = team;
	}
	
	/**
	 * Gets the team.
	 * @return the team
	 */
	public int getTeam()
	{
		return _team;
	}
	
	/**
	 * Sets the wants peace.
	 * @param wantsPeace the new wants peace
	 */
	public void setWantsPeace(int wantsPeace)
	{
		_wantsPeace = wantsPeace;
	}
	
	/**
	 * Gets the wants peace.
	 * @return the wants peace
	 */
	public int getWantsPeace()
	{
		return _wantsPeace;
	}
	
	/**
	 * Checks if is fishing.
	 * @return true, if is fishing
	 */
	public boolean isFishing()
	{
		return _fishing;
	}
	
	/**
	 * Sets the fishing.
	 * @param fishing the new fishing
	 */
	public void setFishing(boolean fishing)
	{
		_fishing = fishing;
	}
	
	/**
	 * Sets the alliance with varka ketra.
	 * @param sideAndLvlOfAlliance the new alliance with varka ketra
	 */
	public void setAllianceWithVarkaKetra(int sideAndLvlOfAlliance)
	{
		// [-5,-1] varka, 0 neutral, [1,5] ketra
		_alliedVarkaKetra = sideAndLvlOfAlliance;
	}
	
	/**
	 * Gets the alliance with varka ketra.
	 * @return the alliance with varka ketra
	 */
	public int getAllianceWithVarkaKetra()
	{
		return _alliedVarkaKetra;
	}
	
	/**
	 * Checks if is allied with varka.
	 * @return true, if is allied with varka
	 */
	public boolean isAlliedWithVarka()
	{
		return (_alliedVarkaKetra < 0);
	}
	
	/**
	 * Checks if is allied with ketra.
	 * @return true, if is allied with ketra
	 */
	public boolean isAlliedWithKetra()
	{
		return (_alliedVarkaKetra > 0);
	}
	
	/**
	 * Send skill list.
	 */
	public void sendSkillList()
	{
		sendSkillList(this);
	}
	
	/**
	 * Send skill list.
	 * @param player the player
	 */
	public void sendSkillList(L2PcInstance player)
	{
		SkillList sl = new SkillList();
		if (player != null)
		{
			for (L2Skill s : player.getAllSkills())
			{
				if (s == null)
				{
					continue;
				}
				if (s.getId() > 9000)
				{
					continue; // Fake skills to change base stats
				}
				sl.addSkill(s.getId(), s.getLevel(), s.isPassive());
			}
		}
		sendPacket(sl);
	}
	
	/**
	 * 1. Add the specified class ID as a subclass (up to the maximum number of <b>three</b>) for this character.<BR>
	 * 2. This method no longer changes the active _classIndex of the player. This is only done by the calling of setActiveClass() method as that should be the only way to do so.
	 * @param classId the class id
	 * @param classIndex the class index
	 * @return boolean subclassAdded
	 */
	public boolean addSubClass(int classId, int classIndex)
	{
		if ((getTotalSubClasses() == 3) || (classIndex == 0))
		{
			return false;
		}
		
		if (getSubClasses().containsKey(classIndex))
		{
			return false;
		}
		
		// Note: Never change _classIndex in any method other than setActiveClass().
		
		SubClass newClass = new SubClass();
		newClass.setClassId(classId);
		newClass.setClassIndex(classIndex);
		
		java.sql.Connection con = null;
		
		try
		{
			// Store the basic info about this new sub-class.
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(ADD_CHAR_SUBCLASS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, newClass.getClassId());
			statement.setLong(3, newClass.getExp());
			statement.setInt(4, newClass.getSp());
			statement.setInt(5, newClass.getLevel());
			statement.setInt(6, newClass.getClassIndex()); // <-- Added
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("WARNING: Could not add character sub class for " + getName() + ": " + e);
			return false;
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
		
		// Commit after database INSERT incase exception is thrown.
		getSubClasses().put(newClass.getClassIndex(), newClass);
		
		if (Config.DEBUG)
		{
			_log.info(getName() + " added class ID " + classId + " as a sub class at index " + classIndex + ".");
		}
		
		ClassId subTemplate = ClassId.values()[classId];
		Collection<L2SkillLearn> skillTree = SkillTreeTable.getInstance().getAllowedSkills(subTemplate);
		
		if (skillTree == null)
		{
			return true;
		}
		
		Map<Integer, L2Skill> prevSkillList = new FastMap<>();
		
		for (L2SkillLearn skillInfo : skillTree)
		{
			if (skillInfo.getMinLevel() <= 40)
			{
				L2Skill prevSkill = prevSkillList.get(skillInfo.getId());
				L2Skill newSkill = SkillTable.getInstance().getInfo(skillInfo.getId(), skillInfo.getLevel());
				
				if ((prevSkill != null) && (prevSkill.getLevel() > newSkill.getLevel()))
				{
					continue;
				}
				
				prevSkillList.put(newSkill.getId(), newSkill);
				storeSkill(newSkill, prevSkill, classIndex);
			}
		}
		
		if (Config.DEBUG)
		{
			_log.info(getName() + " was given " + getAllSkills().length + " skills for their new sub class.");
		}
		
		return true;
	}
	
	/**
	 * 1. Completely erase all existance of the subClass linked to the classIndex.<BR>
	 * 2. Send over the newClassId to addSubClass()to create a new instance on this classIndex.<BR>
	 * 3. Upon Exception, revert the player to their BaseClass to avoid further problems.
	 * @param classIndex the class index
	 * @param newClassId the new class id
	 * @return boolean subclassAdded
	 */
	public boolean modifySubClass(int classIndex, int newClassId)
	{
		int oldClassId = getSubClasses().get(classIndex).getClassId();
		
		if (Config.DEBUG)
		{
			_log.info(getName() + " has requested to modify sub class index " + classIndex + " from class ID " + oldClassId + " to " + newClassId + ".");
		}
		
		java.sql.Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			
			// Remove all henna info stored for this sub-class.
			statement = con.prepareStatement(DELETE_CHAR_HENNAS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();
			
			// Remove all shortcuts info stored for this sub-class.
			statement = con.prepareStatement(DELETE_CHAR_SHORTCUTS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();
			
			// Remove all effects info stored for this sub-class.
			statement = con.prepareStatement(DELETE_SKILL_SAVE);
			statement.setInt(1, getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();
			
			// Remove all skill info stored for this sub-class.
			statement = con.prepareStatement(DELETE_CHAR_SKILLS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();
			
			// Remove all basic info stored about this sub-class.
			statement = con.prepareStatement(DELETE_CHAR_SUBCLASS);
			statement.setInt(1, getObjectId());
			statement.setInt(2, classIndex);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warning("Could not modify sub class for " + getName() + " to class index " + classIndex + ": " + e);
			
			// This must be done in order to maintain data consistency.
			getSubClasses().remove(classIndex);
			return false;
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (Exception e)
			{
			}
		}
		
		getSubClasses().remove(classIndex);
		return addSubClass(newClassId, classIndex);
	}
	
	/**
	 * Checks if is sub class active.
	 * @return true, if is sub class active
	 */
	public boolean isSubClassActive()
	{
		return _classIndex > 0;
	}
	
	/**
	 * Gets the sub classes.
	 * @return the sub classes
	 */
	public Map<Integer, SubClass> getSubClasses()
	{
		if (_subClasses == null)
		{
			_subClasses = new FastMap<>();
		}
		
		return _subClasses;
	}
	
	/**
	 * Gets the total sub classes.
	 * @return the total sub classes
	 */
	public int getTotalSubClasses()
	{
		return getSubClasses().size();
	}
	
	/**
	 * Gets the base class.
	 * @return the base class
	 */
	public int getBaseClass()
	{
		return _baseClass;
	}
	
	/**
	 * Gets the active class.
	 * @return the active class
	 */
	public int getActiveClass()
	{
		return _activeClass;
	}
	
	/**
	 * Gets the class index.
	 * @return the class index
	 */
	public int getClassIndex()
	{
		return _classIndex;
	}
	
	/**
	 * Sets the class template.
	 * @param classId the new class template
	 */
	private void setClassTemplate(int classId)
	{
		_activeClass = classId;
		
		L2PcTemplate t = CharTemplateTable.getInstance().getTemplate(classId);
		
		if (t == null)
		{
			_log.severe("Missing template for classId: " + classId);
			throw new Error();
		}
		
		// Set the template of the L2PcInstance
		setTemplate(t);
	}
	
	/**
	 * Changes the character's class based on the given class index. <BR>
	 * An index of zero specifies the character's original (base) class, while indexes 1-3 specifies the character's sub-classes respectively.
	 * @param classIndex the class index
	 * @return true, if successful
	 */
	public boolean setActiveClass(int classIndex)
	{
		/*
		 * 1. Call store() before modifying _classIndex to avoid skill effects rollover. 2. Register the correct _classId against applied 'classIndex'.
		 */
		store();
		
		if (classIndex == 0)
		{
			setClassTemplate(getBaseClass());
		}
		else
		{
			try
			{
				setClassTemplate(getSubClasses().get(classIndex).getClassId());
			}
			catch (Exception e)
			{
				_log.info("Could not switch " + getName() + "'s sub class to class index " + classIndex + ": " + e);
				return false;
			}
		}
		_classIndex = classIndex;
		
		if (isInParty())
		{
			getParty().recalculatePartyLevel();
		}
		
		/*
		 * Update the character's change in class status. 1. Remove any active cubics from the player. 2. Renovate the characters table in the database with the new class info, storing also buff/effect data. 3. Remove all existing skills. 4. Restore all the learned skills for the current class from
		 * the database. 5. Restore effect/buff data for the new class. 6. Restore henna data for the class, applying the new stat modifiers while removing existing ones. 7. Reset HP/MP/CP stats and send Server->Client character status packet to reflect changes. 8. Restore shortcut data related to
		 * this class. 9. Resend a class change animation effect to broadcast to all nearby players. 10.Unsummon any active servitor from the player.
		 */
		
		if ((getPet() != null) && (getPet() instanceof L2SummonInstance))
		{
			getPet().unSummon(this);
		}
		
		if (getCubics().size() > 0)
		{
			for (L2CubicInstance cubic : getCubics().values())
			{
				cubic.stopAction();
				cubic.cancelDisappear();
			}
			
			getCubics().clear();
		}
		
		for (L2Skill oldSkill : getAllSkills())
		{
			super.removeSkill(oldSkill);
		}
		
		// Yesod: Rebind CursedWeapon passive.
		if (isCursedWeaponEquiped())
		{
			CursedWeaponsManager.getInstance().givePassive(_cursedWeaponEquipedId);
		}
		
		stopAllEffects();
		
		if (isSubClassActive())
		{
			_dwarvenRecipeBook.clear();
			_commonRecipeBook.clear();
		}
		else
		{
			restoreRecipeBook();
		}
		
		// Restore any Death Penalty Buff
		restoreDeathPenaltyBuffLevel();
		
		restoreSkills();
		regiveTemporarySkills();
		rewardSkills();
		restoreEffects();
		sendPacket(new EtcStatusUpdate(this));
		
		// if player has quest 422: Repent Your Sins, remove it
		QuestState st = getQuestState("422_RepentYourSins");
		
		if (st != null)
		{
			st.exitQuest(true);
		}
		
		for (int i = 0; i < 3; i++)
		{
			_henna[i] = null;
		}
		
		restoreHenna();
		sendPacket(new HennaInfo(this));
		
		if (getCurrentHp() > getMaxHp())
		{
			setCurrentHp(getMaxHp());
		}
		if (getCurrentMp() > getMaxMp())
		{
			setCurrentMp(getMaxMp());
		}
		if (getCurrentCp() > getMaxCp())
		{
			setCurrentCp(getMaxCp());
		}
		broadcastUserInfo();
		refreshOverloaded();
		refreshExpertisePenalty();
		
		// Clear resurrect xp calculation
		setExpBeforeDeath(0);
		
		// _macroses.restore();
		// _macroses.sendUpdate();
		_shortCuts.restore();
		sendPacket(new ShortCutInit(this));
		
		broadcastPacket(new SocialAction(getObjectId(), 15));
		
		// decayMe();
		// spawnMe(getX(), getY(), getZ());
		
		return true;
	}
	
	/**
	 * Stop warn user take break.
	 */
	public void stopWarnUserTakeBreak()
	{
		if (_taskWarnUserTakeBreak != null)
		{
			_taskWarnUserTakeBreak.cancel(true);
			_taskWarnUserTakeBreak = null;
		}
	}
	
	/**
	 * Start warn user take break.
	 */
	public void startWarnUserTakeBreak()
	{
		if (_taskWarnUserTakeBreak == null)
		{
			_taskWarnUserTakeBreak = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new WarnUserTakeBreak(), 7200000, 7200000);
		}
	}
	
	/**
	 * Stop rent pet.
	 */
	public void stopRentPet()
	{
		if (_taskRentPet != null)
		{
			// if the rent of a wyvern expires while over a flying zone, tp to down before unmounting
			if (checkLandingState() && (getMountType() == 2))
			{
				teleToLocation(MapRegionTable.TeleportWhereType.Town);
			}
			
			if (setMountType(0)) // this should always be true now, since we teleported already
			{
				_taskRentPet.cancel(true);
				Ride dismount = new Ride(getObjectId(), Ride.ACTION_DISMOUNT, 0);
				sendPacket(dismount);
				broadcastPacket(dismount);
				_taskRentPet = null;
			}
		}
	}
	
	/**
	 * Start rent pet.
	 * @param seconds the seconds
	 */
	public void startRentPet(int seconds)
	{
		if (_taskRentPet == null)
		{
			_taskRentPet = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RentPetTask(), seconds * 1000L, seconds * 1000L);
		}
	}
	
	/**
	 * Checks if is rented pet.
	 * @return true, if is rented pet
	 */
	public boolean isRentedPet()
	{
		if (_taskRentPet != null)
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Stop water task.
	 */
	public void stopWaterTask()
	{
		if (_taskWater != null)
		{
			_taskWater.cancel(false);
			
			_taskWater = null;
			sendPacket(new SetupGauge(2, 0));
		}
	}
	
	/**
	 * Start water task.
	 */
	public void startWaterTask()
	{
		if (!isDead() && (_taskWater == null))
		{
			int timeinwater = 86000;
			
			sendPacket(new SetupGauge(2, timeinwater));
			_taskWater = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new WaterTask(), timeinwater, 1000);
		}
	}
	
	/**
	 * Checks if is in water.
	 * @return true, if is in water
	 */
	public boolean isInWater()
	{
		if (_taskWater != null)
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Check water state.
	 */
	public void checkWaterState()
	{
		// checking if char is over base level of water (sea, rivers)
		if (getZ() > -3793)
		{
			stopWaterTask();
			return;
		}
		
		if (isInsideZone(ZONE_WATER))
		{
			startWaterTask();
		}
	}
	
	/**
	 * On player enter.
	 */
	public void onPlayerEnter()
	{
		startWarnUserTakeBreak();
		
		if (SevenSigns.getInstance().isSealValidationPeriod() || SevenSigns.getInstance().isCompResultsPeriod())
		{
			if (!isGM() && isIn7sDungeon() && (SevenSigns.getInstance().getPlayerCabal(this) != SevenSigns.getInstance().getCabalHighestScore()))
			{
				teleToLocation(MapRegionTable.TeleportWhereType.Town);
				setIsIn7sDungeon(false);
				sendMessage("You have been teleported to the nearest town due to the beginning of the Seal Validation period.");
			}
		}
		else
		{
			if (!isGM() && isIn7sDungeon() && (SevenSigns.getInstance().getPlayerCabal(this) == SevenSigns.CABAL_NULL))
			{
				teleToLocation(MapRegionTable.TeleportWhereType.Town);
				setIsIn7sDungeon(false);
				sendMessage("You have been teleported to the nearest town because you have not signed for any cabal.");
			}
		}
		
		// jail task
		updateJailState();
		
		if (_isInvul)
		{
			sendMessage("Entering world in Invulnerable mode.");
		}
		if (getAppearance().getInvisible())
		{
			sendMessage("Entering world in Invisible mode.");
		}
		if (getMessageRefusal())
		{
			sendMessage("Entering world in Message Refusal mode.");
		}
		
		revalidateZone(true);
	}
	
	/**
	 * Gets the last access.
	 * @return the last access
	 */
	public long getLastAccess()
	{
		return _lastAccess;
	}
	
	/**
	 * Check recom.
	 * @param recsHave the recs have
	 * @param recsLeft the recs left
	 */
	private void checkRecom(int recsHave, int recsLeft)
	{
		Calendar check = Calendar.getInstance();
		check.setTimeInMillis(_lastRecomUpdate);
		check.add(Calendar.DAY_OF_MONTH, 1);
		
		Calendar min = Calendar.getInstance();
		
		_recomHave = recsHave;
		_recomLeft = recsLeft;
		
		if ((getStat().getLevel() < 10) || check.after(min))
		{
			return;
		}
		
		restartRecom();
	}
	
	/**
	 * Restart recom.
	 */
	public void restartRecom()
	{
		if (Config.ALT_RECOMMEND)
		{
			java.sql.Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement(DELETE_CHAR_RECOMS);
				statement.setInt(1, getObjectId());
				statement.execute();
				statement.close();
				
				_recomChars.clear();
			}
			catch (Exception e)
			{
				_log.warning("could not clear char recommendations: " + e);
			}
			finally
			{
				try
				{
					con.close();
				}
				catch (Exception e)
				{
				}
			}
		}
		
		if (getStat().getLevel() < 20)
		{
			_recomLeft = 3;
			_recomHave--;
		}
		else if (getStat().getLevel() < 40)
		{
			_recomLeft = 6;
			_recomHave -= 2;
		}
		else
		{
			_recomLeft = 9;
			_recomHave -= 3;
		}
		if (_recomHave < 0)
		{
			_recomHave = 0;
		}
		
		// If we have to update last update time, but it's now before 13, we should set it to yesterday
		Calendar update = Calendar.getInstance();
		if (update.get(Calendar.HOUR_OF_DAY) < 13)
		{
			update.add(Calendar.DAY_OF_MONTH, -1);
		}
		update.set(Calendar.HOUR_OF_DAY, 13);
		_lastRecomUpdate = update.getTimeInMillis();
	}
	
	@Override
	public void doRevive()
	{
		super.doRevive();
		updateEffectIcons();
		sendPacket(new EtcStatusUpdate(this));
		_reviveRequested = 0;
		_revivePower = 0;
		
		if (isInParty() && getParty().isInDimensionalRift())
		{
			if (!DimensionalRiftManager.getInstance().checkIfInPeaceZone(getX(), getY(), getZ()))
			{
				getParty().getDimensionalRift().memberRessurected(this);
			}
		}
	}
	
	@Override
	public void doRevive(double revivePower)
	{
		// Restore the player's lost experience,
		// depending on the % return of the skill used (based on its power).
		restoreExp(revivePower);
		doRevive();
	}
	
	/**
	 * Revive request.
	 * @param Reviver the reviver
	 * @param skill the skill
	 * @param Pet the pet
	 */
	public void reviveRequest(L2PcInstance Reviver, L2Skill skill, boolean Pet)
	{
		if (_reviveRequested == 1)
		{
			if (_revivePet == Pet)
			{
				Reviver.sendPacket(new SystemMessage(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED)); // Resurrection is already been proposed.
			}
			else
			{
				if (Pet)
				{
					Reviver.sendPacket(new SystemMessage(SystemMessageId.PET_CANNOT_RES)); // A pet cannot be resurrected while it's owner is in the process of resurrecting.
				}
				else
				{
					Reviver.sendPacket(new SystemMessage(SystemMessageId.MASTER_CANNOT_RES)); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
				}
			}
			return;
		}
		if ((Pet && (getPet() != null) && getPet().isDead()) || (!Pet && isDead()))
		{
			_reviveRequested = 1;
			_revivePower = Formulas.getInstance().calculateSkillResurrectRestorePercent(skill.getPower(), Reviver.getWIT());
			_revivePet = Pet;
			sendPacket(new ConfirmDlg(SystemMessageId.RESSURECTION_REQUEST.getId(), Reviver.getName()));
		}
	}
	
	/**
	 * Revive answer.
	 * @param answer the answer
	 */
	public void reviveAnswer(int answer)
	{
		if ((_reviveRequested != 1) || (!isDead() && !_revivePet) || (_revivePet && (getPet() != null) && !getPet().isDead()))
		{
			return;
		}
		if (answer == 1)
		{
			if (!_revivePet)
			{
				if (_revivePower != 0)
				{
					doRevive(_revivePower);
				}
				else
				{
					doRevive();
				}
			}
			else if (getPet() != null)
			{
				if (_revivePower != 0)
				{
					getPet().doRevive(_revivePower);
				}
				else
				{
					getPet().doRevive();
				}
			}
		}
		_reviveRequested = 0;
		_revivePower = 0;
	}
	
	/**
	 * Checks if is revive requested.
	 * @return true, if is revive requested
	 */
	public boolean isReviveRequested()
	{
		return (_reviveRequested == 1);
	}
	
	/**
	 * Checks if is reviving pet.
	 * @return true, if is reviving pet
	 */
	public boolean isRevivingPet()
	{
		return _revivePet;
	}
	
	/**
	 * Removes the reviving.
	 */
	public void removeReviving()
	{
		_reviveRequested = 0;
		_revivePower = 0;
	}
	
	/**
	 * On action request.
	 */
	public void onActionRequest()
	{
		setProtection(false);
	}
	
	/**
	 * Sets the expertise index.
	 * @param expertiseIndex The expertiseIndex to set.
	 */
	public void setExpertiseIndex(int expertiseIndex)
	{
		_expertiseIndex = expertiseIndex;
	}
	
	/**
	 * Gets the expertise index.
	 * @return Returns the expertiseIndex.
	 */
	public int getExpertiseIndex()
	{
		return _expertiseIndex;
	}
	
	@Override
	public final void onTeleported()
	{
		super.onTeleported();
		
		// Force a revalidation
		revalidateZone(true);
		
		if (Config.PLAYER_SPAWN_PROTECTION > 0)
		{
			setProtection(true);
		}
		
		if (Config.ALLOW_WATER)
		{
			checkWaterState();
		}
		
		// Modify the position of the tamed beast if necessary (normal pets are handled by super...though
		// L2PcInstance is the only class that actually has pets!!! )
		if (getTrainedBeast() != null)
		{
			getTrainedBeast().getAI().stopFollow();
			getTrainedBeast().teleToLocation(getPosition().getX() + Rnd.get(-100, 100), getPosition().getY() + Rnd.get(-100, 100), getPosition().getZ(), false);
			getTrainedBeast().getAI().startFollow(this);
		}
		
	}
	
	@Override
	public final boolean updatePosition(int gameTicks)
	{
		// Disables custom movement for L2PCInstance when Old Synchronization is selected
		if (Config.COORD_SYNCHRONIZE == -1)
		{
			return super.updatePosition(gameTicks);
		}
		
		// Get movement data
		MoveData m = _move;
		
		if (_move == null)
		{
			return true;
		}
		
		if (!isVisible())
		{
			_move = null;
			return true;
		}
		
		// Check if the position has alreday be calculated
		if (m._moveTimestamp == 0)
		{
			m._moveTimestamp = m._moveStartTime;
		}
		
		// Check if the position has alreday be calculated
		if (m._moveTimestamp == gameTicks)
		{
			return false;
		}
		
		double dx = m._xDestination - getX();
		double dy = m._yDestination - getY();
		double dz = m._zDestination - getZ();
		int distPassed = ((int) getStat().getMoveSpeed() * (gameTicks - m._moveTimestamp)) / GameTimeController.TICKS_PER_SECOND;
		double distFraction = (distPassed) / Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
		// if (Config.DEVELOPER) System.out.println("Move Ticks:" + (gameTicks - m._moveTimestamp) + ", distPassed:" + distPassed + ", distFraction:" + distFraction);
		
		if (distFraction > 1)
		{
			// Set the position of the L2Character to the destination
			super.setXYZ(m._xDestination, m._yDestination, m._zDestination);
		}
		else
		{
			// Set the position of the L2Character to estimated after parcial move
			super.setXYZ(getX() + (int) ((dx * distFraction) + 0.5), getY() + (int) ((dy * distFraction) + 0.5), getZ() + (int) (dz * distFraction));
		}
		
		// Set the timer of last position update to now
		m._moveTimestamp = gameTicks;
		
		revalidateZone(false);
		
		return (distFraction > 1);
	}
	
	/**
	 * Sets the last client position.
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 */
	public void setLastClientPosition(int x, int y, int z)
	{
		_lastClientPosition.setXYZ(x, y, z);
	}
	
	/**
	 * Check last client position.
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 * @return true, if successful
	 */
	public boolean checkLastClientPosition(int x, int y, int z)
	{
		return _lastClientPosition.equals(x, y, z);
	}
	
	/**
	 * Gets the last client distance.
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 * @return the last client distance
	 */
	public int getLastClientDistance(int x, int y, int z)
	{
		double dx = (x - _lastClientPosition.getX());
		double dy = (y - _lastClientPosition.getY());
		double dz = (z - _lastClientPosition.getZ());
		
		return (int) Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
	}
	
	/**
	 * Sets the last server position.
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 */
	public void setLastServerPosition(int x, int y, int z)
	{
		_lastServerPosition.setXYZ(x, y, z);
	}
	
	/**
	 * Check last server position.
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 * @return true, if successful
	 */
	public boolean checkLastServerPosition(int x, int y, int z)
	{
		return _lastServerPosition.equals(x, y, z);
	}
	
	/**
	 * Gets the last server distance.
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 * @return the last server distance
	 */
	public int getLastServerDistance(int x, int y, int z)
	{
		double dx = (x - _lastServerPosition.getX());
		double dy = (y - _lastServerPosition.getY());
		double dz = (z - _lastServerPosition.getZ());
		
		return (int) Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
	}
	
	@Override
	public void addExpAndSp(long addToExp, int addToSp)
	{
		getStat().addExpAndSp(addToExp, addToSp);
	}
	
	/**
	 * Removes the exp and sp.
	 * @param removeExp the remove exp
	 * @param removeSp the remove sp
	 */
	public void removeExpAndSp(long removeExp, int removeSp)
	{
		getStat().removeExpAndSp(removeExp, removeSp);
	}
	
	@Override
	public void reduceCurrentHp(double i, L2Character attacker)
	{
		getStatus().reduceHp(i, attacker);
		
		// notify the tamed beast of attacks
		if (getTrainedBeast() != null)
		{
			getTrainedBeast().onOwnerGotAttacked(attacker);
		}
	}
	
	@Override
	public void reduceCurrentHp(double value, L2Character attacker, boolean awake)
	{
		getStatus().reduceHp(value, attacker, awake);
		
		// notify the tamed beast of attacks
		if (getTrainedBeast() != null)
		{
			getTrainedBeast().onOwnerGotAttacked(attacker);
		}
	}
	
	/**
	 * Broadcast snoop.
	 * @param type the type
	 * @param name the name
	 * @param _text the _text
	 */
	public void broadcastSnoop(int type, String name, String _text)
	{
		if (_snoopListener.size() > 0)
		{
			Snoop sn = new Snoop(getObjectId(), getName(), type, name, _text);
			
			for (L2PcInstance pci : _snoopListener)
			{
				if (pci != null)
				{
					pci.sendPacket(sn);
				}
			}
		}
	}
	
	/**
	 * Adds the snooper.
	 * @param pci the pci
	 */
	public void addSnooper(L2PcInstance pci)
	{
		if (!_snoopListener.contains(pci))
		{
			_snoopListener.add(pci);
		}
	}
	
	/**
	 * Removes the snooper.
	 * @param pci the pci
	 */
	public void removeSnooper(L2PcInstance pci)
	{
		_snoopListener.remove(pci);
	}
	
	/**
	 * Adds the snooped.
	 * @param pci the pci
	 */
	public void addSnooped(L2PcInstance pci)
	{
		if (!_snoopedPlayer.contains(pci))
		{
			_snoopedPlayer.add(pci);
		}
	}
	
	/**
	 * Removes the snooped.
	 * @param pci the pci
	 */
	public void removeSnooped(L2PcInstance pci)
	{
		_snoopedPlayer.remove(pci);
	}
	
	/**
	 * Adds the bypass.
	 * @param bypass the bypass
	 */
	public synchronized void addBypass(String bypass)
	{
		if (bypass == null)
		{
			return;
		}
		_validBypass.add(bypass);
		// _log.warning("[BypassAdd]"+getName()+" '"+bypass+"'");
	}
	
	/**
	 * Adds the bypass2.
	 * @param bypass the bypass
	 */
	public synchronized void addBypass2(String bypass)
	{
		if (bypass == null)
		{
			return;
		}
		_validBypass2.add(bypass);
		// _log.warning("[BypassAdd]"+getName()+" '"+bypass+"'");
	}
	
	/**
	 * Validate bypass.
	 * @param cmd the cmd
	 * @return true, if successful
	 */
	public synchronized boolean validateBypass(String cmd)
	{
		if (!Config.BYPASS_VALIDATION)
		{
			return true;
		}
		
		for (String bp : _validBypass)
		{
			if (bp == null)
			{
				continue;
			}
			
			// _log.warning("[BypassValidation]"+getName()+" '"+bp+"'");
			if (bp.equals(cmd))
			{
				return true;
			}
		}
		
		for (String bp : _validBypass2)
		{
			if (bp == null)
			{
				continue;
			}
			
			// _log.warning("[BypassValidation]"+getName()+" '"+bp+"'");
			if (cmd.startsWith(bp))
			{
				return true;
			}
		}
		
		_log.warning("[L2PcInstance] player [" + getName() + "] sent invalid bypass '" + cmd + "', ban this player!");
		return false;
	}
	
	/**
	 * Validate item manipulation.
	 * @param objectId the object id
	 * @param action the action
	 * @return true, if successful
	 */
	public boolean validateItemManipulation(int objectId, String action)
	{
		L2ItemInstance item = getInventory().getItemByObjectId(objectId);
		
		if ((item == null) || (item.getOwnerId() != getObjectId()))
		{
			_log.finest(getObjectId() + ": player tried to " + action + " item he is not owner of");
			return false;
		}
		
		// Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
		if (((getPet() != null) && (getPet().getControlItemId() == objectId)) || (getMountObjectID() == objectId))
		{
			if (Config.DEBUG)
			{
				_log.finest(getObjectId() + ": player tried to " + action + " item controling pet");
			}
			
			return false;
		}
		
		if ((getActiveEnchantItem() != null) && (getActiveEnchantItem().getObjectId() == objectId))
		{
			if (Config.DEBUG)
			{
				_log.finest(getObjectId() + ":player tried to " + action + " an enchant scroll he was using");
			}
			
			return false;
		}
		
		if (CursedWeaponsManager.getInstance().isCursed(item.getItemId()))
		{
			// can not trade a cursed weapon
			return false;
		}
		
		if (item.isWear())
		{
			// cannot drop/trade wear-items
			return false;
		}
		
		return true;
	}
	
	/**
	 * Clear bypass.
	 */
	public synchronized void clearBypass()
	{
		_validBypass.clear();
		_validBypass2.clear();
	}
	
	/**
	 * Checks if is in boat.
	 * @return Returns the inBoat.
	 */
	public boolean isInBoat()
	{
		return _inBoat;
	}
	
	/**
	 * Sets the in boat.
	 * @param inBoat The inBoat to set.
	 */
	public void setInBoat(boolean inBoat)
	{
		_inBoat = inBoat;
	}
	
	/**
	 * Gets the boat.
	 * @return the boat
	 */
	public L2BoatInstance getBoat()
	{
		return _boat;
	}
	
	/**
	 * Sets the boat.
	 * @param boat the new boat
	 */
	public void setBoat(L2BoatInstance boat)
	{
		_boat = boat;
	}
	
	/**
	 * Sets the in crystallize.
	 * @param inCrystallize the new in crystallize
	 */
	public void setInCrystallize(boolean inCrystallize)
	{
		_inCrystallize = inCrystallize;
	}
	
	/**
	 * Checks if is in crystallize.
	 * @return true, if is in crystallize
	 */
	public boolean isInCrystallize()
	{
		return _inCrystallize;
	}
	
	/**
	 * Gets the in boat position.
	 * @return the in boat position
	 */
	public Point3D getInBoatPosition()
	{
		return _inBoatPosition;
	}
	
	/**
	 * Sets the in boat position.
	 * @param pt the new in boat position
	 */
	public void setInBoatPosition(Point3D pt)
	{
		_inBoatPosition = pt;
	}
	
	/**
	 * Manage the delete task of a L2PcInstance (Leave Party, Unsummon pet, Save its inventory in the database, Remove it from the world...).<BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>If the L2PcInstance is in observer mode, set its position to its position before entering in observer mode</li> <li>Set the online Flag to True or False and update the characters table of the database with online status and lastAccess</li> <li>Stop the HP/MP/CP Regeneration task</li> <li>
	 * Cancel Crafting, Attak or Cast</li> <li>Remove the L2PcInstance from the world</li> <li>Stop Party and Unsummon Pet</li> <li>Update database with items in its inventory and remove them from the world</li> <li>Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then
	 * cancel Attak or Cast and notify AI</li> <li>Close the connection with the client</li><BR>
	 */
	public void deleteMe()
	{
		// Check if the L2PcInstance is in observer mode to set its position to its position before entering in observer mode
		if (inObserverMode())
		{
			setXYZ(_obsX, _obsY, _obsZ);
		}
		
		// Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout)
		try
		{
			setOnlineStatus(false);
		}
		catch (Throwable t)
		{
			_log.log(Level.SEVERE, "deleteMe()", t);
		}
		
		// Stop the HP/MP/CP Regeneration task (scheduled tasks)
		try
		{
			stopAllTimers();
		}
		catch (Throwable t)
		{
			_log.log(Level.SEVERE, "deleteMe()", t);
		}
		
		// Stop crafting, if in progress
		try
		{
			RecipeController.getInstance().requestMakeItemAbort(this);
		}
		catch (Throwable t)
		{
			_log.log(Level.SEVERE, "deleteMe()", t);
		}
		
		// Cancel Attak or Cast
		try
		{
			setTarget(null);
		}
		catch (Throwable t)
		{
			_log.log(Level.SEVERE, "deleteMe()", t);
		}
		
		// Remove from world regions zones
		if (getWorldRegion() != null)
		{
			getWorldRegion().removeFromZones(this);
		}
		
		try
		{
			if (_forceBuff != null)
			{
				_forceBuff.delete();
			}
			for (L2Character character : getKnownList().getKnownCharacters())
			{
				if ((character.getForceBuff() != null) && (character.getForceBuff().getTarget() == this))
				{
					character.abortCast();
				}
			}
		}
		catch (Throwable t)
		{
			_log.log(Level.SEVERE, "deleteMe()", t);
		}
		
		// Remove the L2PcInstance from the world
		if (isVisible())
		{
			try
			{
				decayMe();
			}
			catch (Throwable t)
			{
				_log.log(Level.SEVERE, "deleteMe()", t);
			}
		}
		
		// If a Party is in progress, leave it
		if (isInParty())
		{
			try
			{
				leaveParty();
			}
			catch (Throwable t)
			{
				_log.log(Level.SEVERE, "deleteMe()", t);
			}
		}
		
		// If the L2PcInstance has Pet, unsummon it
		if (getPet() != null)
		{
			try
			{
				getPet().unSummon(this);
			}
			catch (Throwable t)
			{
				_log.log(Level.SEVERE, "deleteMe()", t);
			}// returns pet to control item
		}
		
		if ((getClanId() != 0) && (getClan() != null))
		{
			// set the status for pledge member list to OFFLINE
			try
			{
				L2ClanMember clanMember = getClan().getClanMember(getName());
				if (clanMember != null)
				{
					clanMember.setPlayerInstance(null);
				}
			}
			catch (Throwable t)
			{
				_log.log(Level.SEVERE, "deleteMe()", t);
			}
		}
		
		if (getActiveRequester() != null)
		{
			// deals with sudden exit in the middle of transaction
			setActiveRequester(null);
		}
		
		// If the L2PcInstance is a GM, remove it from the GM List
		if (isGM())
		{
			try
			{
				GmListTable.getInstance().deleteGm(this);
			}
			catch (Throwable t)
			{
				_log.log(Level.SEVERE, "deleteMe()", t);
			}
		}
		
		// Update database with items in its inventory and remove them from the world
		try
		{
			getInventory().deleteMe();
		}
		catch (Throwable t)
		{
			_log.log(Level.SEVERE, "deleteMe()", t);
		}
		
		// Update database with items in its warehouse and remove them from the world
		try
		{
			clearWarehouse();
		}
		catch (Throwable t)
		{
			_log.log(Level.SEVERE, "deleteMe()", t);
		}
		if (Config.WAREHOUSE_CACHE)
		{
			WarehouseCacheManager.getInstance().remCacheTask(this);
		}
		
		// Update database with items in its freight and remove them from the world
		try
		{
			getFreight().deleteMe();
		}
		catch (Throwable t)
		{
			_log.log(Level.SEVERE, "deleteMe()", t);
		}
		
		// Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI
		try
		{
			getKnownList().removeAllKnownObjects();
		}
		catch (Throwable t)
		{
			_log.log(Level.SEVERE, "deleteMe()", t);
		}
		
		// Close the connection with the client
		closeNetConnection();
		
		// remove from flood protector
		FloodProtector.getInstance().removePlayer(getObjectId());
		
		if (getClanId() > 0)
		{
			getClan().broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);
			// ClanTable.getInstance().getClan(getClanId()).broadcastToOnlineMembers(new PledgeShowMemberListAdd(this));
		}
		
		for (L2PcInstance player : _snoopedPlayer)
		{
			player.removeSnooper(this);
		}
		
		for (L2PcInstance player : _snoopListener)
		{
			player.removeSnooped(this);
		}
		
		// Remove L2Object object from _allObjects of L2World
		L2World.getInstance().removeObject(this);
	}
	
	/** The _fish. */
	private FishData _fish;
	
	/*
	 * startFishing() was stripped of any pre-fishing related checks, namely the fishing zone check. Also worthy of note is the fact the code to find the hook landing position was also striped. The stripped code was moved into fishing.java. In my opinion it makes more sense for it to be there since
	 * all other skill related checks were also there. Last but not least, moving the zone check there, fixed a bug where baits would always be consumed no matter if fishing actualy took place. startFishing() now takes up 3 arguments, wich are acurately described as being the hook landing
	 * coordinates.
	 */
	/**
	 * Start fishing.
	 * @param _x the _x
	 * @param _y the _y
	 * @param _z the _z
	 */
	public void startFishing(int _x, int _y, int _z)
	{
		stopMove(null);
		setIsImobilised(true);
		_fishing = true;
		_fishx = _x;
		_fishy = _y;
		_fishz = _z;
		broadcastUserInfo();
		// Starts fishing
		int lvl = GetRandomFishLvl();
		int group = GetRandomGroup();
		int type = GetRandomFishType(group);
		List<FishData> fishs = FishTable.getInstance().getfish(lvl, type, group);
		if ((fishs == null) || (fishs.size() == 0))
		{
			sendMessage("Error - Fishes are not definied");
			EndFishing(false);
			return;
		}
		int check = Rnd.get(fishs.size());
		// Use a copy constructor else the fish data may be over-written below
		_fish = new FishData(fishs.get(check));
		fishs.clear();
		fishs = null;
		sendPacket(new SystemMessage(SystemMessageId.CAST_LINE_AND_START_FISHING));
		ExFishingStart efs = null;
		if (!GameTimeController.getInstance().isNowNight() && _lure.isNightLure())
		{
			_fish.setType(-1);
		}
		// sendMessage("Hook x,y: " + _x + "," + _y + " - Water Z, Player Z:" + _z + ", " + getZ()); //debug line, uncoment to show coordinates used in fishing.
		efs = new ExFishingStart(this, _fish.getType(), _x, _y, _z, _lure.isNightLure());
		broadcastPacket(efs);
		StartLookingForFishTask();
	}
	
	/**
	 * Stop looking for fish task.
	 */
	public void stopLookingForFishTask()
	{
		if (_taskforfish != null)
		{
			_taskforfish.cancel(false);
			_taskforfish = null;
		}
	}
	
	/**
	 * Start looking for fish task.
	 */
	public void StartLookingForFishTask()
	{
		if (!isDead() && (_taskforfish == null))
		{
			int checkDelay = 0;
			boolean isNoob = false;
			boolean isUpperGrade = false;
			
			if (_lure != null)
			{
				int lureid = _lure.getItemId();
				isNoob = _fish.getGroup() == 0;
				isUpperGrade = _fish.getGroup() == 2;
				if ((lureid == 6519) || (lureid == 6522) || (lureid == 6525) || (lureid == 8505) || (lureid == 8508) || (lureid == 8511))
				{
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (1.33)));
				}
				else if ((lureid == 6520) || (lureid == 6523) || (lureid == 6526) || ((lureid >= 8505) && (lureid <= 8513)) || ((lureid >= 7610) && (lureid <= 7613)) || ((lureid >= 7807) && (lureid <= 7809)) || ((lureid >= 8484) && (lureid <= 8486)))
				{
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (1.00)));
				}
				else if ((lureid == 6521) || (lureid == 6524) || (lureid == 6527) || (lureid == 8507) || (lureid == 8510) || (lureid == 8513))
				{
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * (0.66)));
				}
			}
			_taskforfish = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new LookingForFishTask(_fish.getWaitTime(), _fish.getFishGuts(), _fish.getType(), isNoob, isUpperGrade), 10000, checkDelay);
		}
	}
	
	/**
	 * Gets the random group.
	 * @return the int
	 */
	private int GetRandomGroup()
	{
		switch (_lure.getItemId())
		{
			case 7807: // green for beginners
			case 7808: // purple for beginners
			case 7809: // yellow for beginners
			case 8486: // prize-winning for beginners
				return 0;
			case 8485: // prize-winning luminous
			case 8506: // green luminous
			case 8509: // purple luminous
			case 8512: // yellow luminous
				return 2;
			default:
				return 1;
		}
	}
	
	/**
	 * Gets the random fish type.
	 * @param group the group
	 * @return the int
	 */
	private int GetRandomFishType(int group)
	{
		int check = Rnd.get(100);
		int type = 1;
		switch (group)
		{
			case 0: // fish for novices
				switch (_lure.getItemId())
				{
					case 7807: // green lure, preferred by fast-moving (nimble) fish (type 5)
						if (check <= 54)
						{
							type = 5;
						}
						else if (check <= 77)
						{
							type = 4;
						}
						else
						{
							type = 6;
						}
						break;
					case 7808: // purple lure, preferred by fat fish (type 4)
						if (check <= 54)
						{
							type = 4;
						}
						else if (check <= 77)
						{
							type = 6;
						}
						else
						{
							type = 5;
						}
						break;
					case 7809: // yellow lure, preferred by ugly fish (type 6)
						if (check <= 54)
						{
							type = 6;
						}
						else if (check <= 77)
						{
							type = 5;
						}
						else
						{
							type = 4;
						}
						break;
					case 8486: // prize-winning fishing lure for beginners
						if (check <= 33)
						{
							type = 4;
						}
						else if (check <= 66)
						{
							type = 5;
						}
						else
						{
							type = 6;
						}
						break;
				}
				break;
			case 1: // normal fish
				switch (_lure.getItemId())
				{
					case 7610:
					case 7611:
					case 7612:
					case 7613:
						type = 3;
						break;
					case 6519: // all theese lures (green) are prefered by fast-moving (nimble) fish (type 1)
					case 8505:
					case 6520:
					case 6521:
					case 8507:
						if (check <= 54)
						{
							type = 1;
						}
						else if (check <= 74)
						{
							type = 0;
						}
						else if (check <= 94)
						{
							type = 2;
						}
						else
						{
							type = 3;
						}
						break;
					case 6522: // all theese lures (purple) are prefered by fat fish (type 0)
					case 8508:
					case 6523:
					case 6524:
					case 8510:
						if (check <= 54)
						{
							type = 0;
						}
						else if (check <= 74)
						{
							type = 1;
						}
						else if (check <= 94)
						{
							type = 2;
						}
						else
						{
							type = 3;
						}
						break;
					case 6525: // all theese lures (yellow) are prefered by ugly fish (type 2)
					case 8511:
					case 6526:
					case 6527:
					case 8513:
						if (check <= 55)
						{
							type = 2;
						}
						else if (check <= 74)
						{
							type = 1;
						}
						else if (check <= 94)
						{
							type = 0;
						}
						else
						{
							type = 3;
						}
						break;
					case 8484: // prize-winning fishing lure
						if (check <= 33)
						{
							type = 0;
						}
						else if (check <= 66)
						{
							type = 1;
						}
						else
						{
							type = 2;
						}
						break;
				}
				break;
			case 2: // upper grade fish, luminous lure
				switch (_lure.getItemId())
				{
					case 8506: // green lure, preferred by fast-moving (nimble) fish (type 8)
						if (check <= 54)
						{
							type = 8;
						}
						else if (check <= 77)
						{
							type = 7;
						}
						else
						{
							type = 9;
						}
						break;
					case 8509: // purple lure, preferred by fat fish (type 7)
						if (check <= 54)
						{
							type = 7;
						}
						else if (check <= 77)
						{
							type = 9;
						}
						else
						{
							type = 8;
						}
						break;
					case 8512: // yellow lure, preferred by ugly fish (type 9)
						if (check <= 54)
						{
							type = 9;
						}
						else if (check <= 77)
						{
							type = 8;
						}
						else
						{
							type = 7;
						}
						break;
					case 8485: // prize-winning fishing lure
						if (check <= 33)
						{
							type = 7;
						}
						else if (check <= 66)
						{
							type = 8;
						}
						else
						{
							type = 9;
						}
						break;
				}
		}
		return type;
	}
	
	/**
	 * Gets the random fish lvl.
	 * @return the int
	 */
	private int GetRandomFishLvl()
	{
		L2Effect[] effects = getAllEffects();
		int skilllvl = getSkillLevel(1315);
		for (L2Effect e : effects)
		{
			if (e.getSkill().getId() == 2274)
			{
				skilllvl = (int) e.getSkill().getPower(this);
			}
		}
		if (skilllvl <= 0)
		{
			return 1;
		}
		int randomlvl;
		int check = Rnd.get(100);
		
		if (check <= 50)
		{
			randomlvl = skilllvl;
		}
		else if (check <= 85)
		{
			randomlvl = skilllvl - 1;
			if (randomlvl <= 0)
			{
				randomlvl = 1;
			}
		}
		else
		{
			randomlvl = skilllvl + 1;
			if (randomlvl > 27)
			{
				randomlvl = 27;
			}
		}
		
		return randomlvl;
	}
	
	/**
	 * Start fish combat.
	 * @param isNoob the is noob
	 * @param isUpperGrade the is upper grade
	 */
	public void StartFishCombat(boolean isNoob, boolean isUpperGrade)
	{
		_fishCombat = new L2Fishing(this, _fish, isNoob, isUpperGrade);
	}
	
	/**
	 * End fishing.
	 * @param win the win
	 */
	public void EndFishing(boolean win)
	{
		ExFishingEnd efe = new ExFishingEnd(win, this);
		broadcastPacket(efe);
		_fishing = false;
		_fishx = 0;
		_fishy = 0;
		_fishz = 0;
		broadcastUserInfo();
		if (_fishCombat == null)
		{
			sendPacket(new SystemMessage(SystemMessageId.BAIT_LOST_FISH_GOT_AWAY));
		}
		_fishCombat = null;
		_lure = null;
		// Ends fishing
		sendPacket(new SystemMessage(SystemMessageId.REEL_LINE_AND_STOP_FISHING));
		setIsImobilised(false);
		stopLookingForFishTask();
	}
	
	/**
	 * Gets the fish combat.
	 * @return the l2 fishing
	 */
	public L2Fishing GetFishCombat()
	{
		return _fishCombat;
	}
	
	/**
	 * Gets the fishx.
	 * @return the int
	 */
	public int GetFishx()
	{
		return _fishx;
	}
	
	/**
	 * Gets the fishy.
	 * @return the int
	 */
	public int GetFishy()
	{
		return _fishy;
	}
	
	/**
	 * Gets the fishz.
	 * @return the int
	 */
	public int GetFishz()
	{
		return _fishz;
	}
	
	/**
	 * Sets the lure.
	 * @param lure the lure
	 */
	public void SetLure(L2ItemInstance lure)
	{
		_lure = lure;
	}
	
	/**
	 * Gets the lure.
	 * @return the l2 item instance
	 */
	public L2ItemInstance GetLure()
	{
		return _lure;
	}
	
	/**
	 * Gets the inventory limit.
	 * @return the int
	 */
	public int GetInventoryLimit()
	{
		int ivlim;
		if (isGM())
		{
			ivlim = Config.INVENTORY_MAXIMUM_GM;
		}
		else if (getRace() == Race.dwarf)
		{
			ivlim = Config.INVENTORY_MAXIMUM_DWARF;
		}
		else
		{
			ivlim = Config.INVENTORY_MAXIMUM_NO_DWARF;
		}
		ivlim += (int) getStat().calcStat(Stats.INV_LIM, 0, null, null);
		
		return ivlim;
	}
	
	/**
	 * Gets the ware house limit.
	 * @return the int
	 */
	public int GetWareHouseLimit()
	{
		int whlim;
		if (getRace() == Race.dwarf)
		{
			whlim = Config.WAREHOUSE_SLOTS_DWARF;
		}
		else
		{
			whlim = Config.WAREHOUSE_SLOTS_NO_DWARF;
		}
		whlim += (int) getStat().calcStat(Stats.WH_LIM, 0, null, null);
		
		return whlim;
	}
	
	/**
	 * Gets the private sell store limit.
	 * @return the int
	 */
	public int GetPrivateSellStoreLimit()
	{
		int pslim;
		if (getRace() == Race.dwarf)
		{
			pslim = Config.MAX_PVTSTORE_SLOTS_DWARF;
		}
		else
		{
			pslim = Config.MAX_PVTSTORE_SLOTS_OTHER;
		}
		pslim += (int) getStat().calcStat(Stats.P_SELL_LIM, 0, null, null);
		
		return pslim;
	}
	
	/**
	 * Gets the private buy store limit.
	 * @return the int
	 */
	public int GetPrivateBuyStoreLimit()
	{
		int pblim;
		if (getRace() == Race.dwarf)
		{
			pblim = Config.MAX_PVTSTORE_SLOTS_DWARF;
		}
		else
		{
			pblim = Config.MAX_PVTSTORE_SLOTS_OTHER;
		}
		pblim += (int) getStat().calcStat(Stats.P_BUY_LIM, 0, null, null);
		
		return pblim;
	}
	
	/**
	 * Gets the freight limit.
	 * @return the int
	 */
	public int GetFreightLimit()
	{
		return Config.FREIGHT_SLOTS + (int) getStat().calcStat(Stats.FREIGHT_LIM, 0, null, null);
	}
	
	/**
	 * Gets the dwarf recipe limit.
	 * @return the int
	 */
	public int GetDwarfRecipeLimit()
	{
		int recdlim = Config.DWARF_RECIPE_LIMIT;
		recdlim += (int) getStat().calcStat(Stats.REC_D_LIM, 0, null, null);
		return recdlim;
	}
	
	/**
	 * Gets the common recipe limit.
	 * @return the int
	 */
	public int GetCommonRecipeLimit()
	{
		int recclim = Config.COMMON_RECIPE_LIMIT;
		recclim += (int) getStat().calcStat(Stats.REC_C_LIM, 0, null, null);
		return recclim;
	}
	
	/**
	 * Sets the mount object id.
	 * @param newID the new mount object id
	 */
	public void setMountObjectID(int newID)
	{
		_mountObjectID = newID;
	}
	
	/**
	 * Gets the mount object id.
	 * @return the mount object id
	 */
	public int getMountObjectID()
	{
		return _mountObjectID;
	}
	
	/** The _lure. */
	private L2ItemInstance _lure = null;
	
	/**
	 * Get the current skill in use or return null.
	 * @return the current skill
	 */
	public SkillDat getCurrentSkill()
	{
		return _currentSkill;
	}
	
	/**
	 * Create a new SkillDat object and set the player _currentSkill.
	 * @param currentSkill the current skill
	 * @param ctrlPressed the ctrl pressed
	 * @param shiftPressed the shift pressed
	 */
	public void setCurrentSkill(L2Skill currentSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (currentSkill == null)
		{
			if (Config.DEBUG)
			{
				_log.info("Setting current skill: NULL for " + getName() + ".");
			}
			
			_currentSkill = null;
			return;
		}
		
		if (Config.DEBUG)
		{
			_log.info("Setting current skill: " + currentSkill.getName() + " (ID: " + currentSkill.getId() + ") for " + getName() + ".");
		}
		
		_currentSkill = new SkillDat(currentSkill, ctrlPressed, shiftPressed);
	}
	
	/**
	 * Gets the queued skill.
	 * @return the queued skill
	 */
	public SkillDat getQueuedSkill()
	{
		return _queuedSkill;
	}
	
	/**
	 * Create a new SkillDat object and queue it in the player _queuedSkill.
	 * @param queuedSkill the queued skill
	 * @param ctrlPressed the ctrl pressed
	 * @param shiftPressed the shift pressed
	 */
	public void setQueuedSkill(L2Skill queuedSkill, boolean ctrlPressed, boolean shiftPressed)
	{
		if (queuedSkill == null)
		{
			if (Config.DEBUG)
			{
				_log.info("Setting queued skill: NULL for " + getName() + ".");
			}
			
			_queuedSkill = null;
			return;
		}
		
		if (Config.DEBUG)
		{
			_log.info("Setting queued skill: " + queuedSkill.getName() + " (ID: " + queuedSkill.getId() + ") for " + getName() + ".");
		}
		
		_queuedSkill = new SkillDat(queuedSkill, ctrlPressed, shiftPressed);
	}
	
	/**
	 * Checks if is in jail.
	 * @return true, if is in jail
	 */
	public boolean isInJail()
	{
		return _inJail;
	}
	
	/**
	 * Sets the in jail.
	 * @param state the new in jail
	 */
	public void setInJail(boolean state)
	{
		_inJail = state;
	}
	
	/**
	 * Sets the in jail.
	 * @param state the state
	 * @param delayInMinutes the delay in minutes
	 */
	public void setInJail(boolean state, int delayInMinutes)
	{
		_inJail = state;
		_jailTimer = 0;
		// Remove the task if any
		stopJailTask(false);
		
		if (_inJail)
		{
			if (delayInMinutes > 0)
			{
				_jailTimer = delayInMinutes * 60000L; // in millisec
				
				// start the countdown
				_jailTask = ThreadPoolManager.getInstance().scheduleGeneral(new JailTask(this), _jailTimer);
				sendMessage("You are in jail for " + delayInMinutes + " minutes.");
			}
			
			// Open a Html message to inform the player
			NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
			String jailInfos = HtmCache.getInstance().getHtm("data/html/jail_in.htm");
			if (jailInfos != null)
			{
				htmlMsg.setHtml(jailInfos);
			}
			else
			{
				htmlMsg.setHtml("<html><body>You have been put in jail by an admin.</body></html>");
			}
			sendPacket(htmlMsg);
			
			teleToLocation(-114356, -249645, -2984, true); // Jail
		}
		else
		{
			// Open a Html message to inform the player
			NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
			String jailInfos = HtmCache.getInstance().getHtm("data/html/jail_out.htm");
			if (jailInfos != null)
			{
				htmlMsg.setHtml(jailInfos);
			}
			else
			{
				htmlMsg.setHtml("<html><body>You are free for now, respect server rules!</body></html>");
			}
			sendPacket(htmlMsg);
			
			teleToLocation(17836, 170178, -3507, true); // Floran
		}
		
		// store in database
		storeCharBase();
	}
	
	/**
	 * Gets the jail timer.
	 * @return the jail timer
	 */
	public long getJailTimer()
	{
		return _jailTimer;
	}
	
	/**
	 * Sets the jail timer.
	 * @param time the new jail timer
	 */
	public void setJailTimer(long time)
	{
		_jailTimer = time;
	}
	
	/**
	 * Update jail state.
	 */
	private void updateJailState()
	{
		if (isInJail())
		{
			// If jail time is elapsed, free the player
			if (_jailTimer > 0)
			{
				// restart the countdown
				_jailTask = ThreadPoolManager.getInstance().scheduleGeneral(new JailTask(this), _jailTimer);
				sendMessage("You are still in jail for " + (_jailTimer / 60000) + " minutes.");
			}
			
			// If player escaped, put him back in jail
			if (!isInsideZone(ZONE_JAIL))
			{
				teleToLocation(-114356, -249645, -2984, true);
			}
		}
	}
	
	/**
	 * Stop jail task.
	 * @param save the save
	 */
	public void stopJailTask(boolean save)
	{
		if (_jailTask != null)
		{
			if (save)
			{
				long delay = _jailTask.getDelay(TimeUnit.MILLISECONDS);
				if (delay < 0)
				{
					delay = 0;
				}
				setJailTimer(delay);
			}
			_jailTask.cancel(false);
			_jailTask = null;
		}
	}
	
	/**
	 * The Class JailTask.
	 */
	private class JailTask implements Runnable
	{
		
		/** The _player. */
		L2PcInstance _player;
		
		/** The _started at. */
		@SuppressWarnings("unused")
		protected long _startedAt;
		
		/**
		 * Instantiates a new jail task.
		 * @param player the player
		 */
		protected JailTask(L2PcInstance player)
		{
			_player = player;
			_startedAt = System.currentTimeMillis();
		}
		
		@Override
		public void run()
		{
			_player.setInJail(false, 0);
		}
	}
	
	/**
	 * Gets the power grade.
	 * @return the power grade
	 */
	public int getPowerGrade()
	{
		return _powerGrade;
	}
	
	/**
	 * Sets the power grade.
	 * @param power the new power grade
	 */
	public void setPowerGrade(int power)
	{
		_powerGrade = power;
	}
	
	/**
	 * Checks if is cursed weapon equiped.
	 * @return true, if is cursed weapon equiped
	 */
	public boolean isCursedWeaponEquiped()
	{
		return _cursedWeaponEquipedId != 0;
	}
	
	/**
	 * Sets the cursed weapon equiped id.
	 * @param value the new cursed weapon equiped id
	 */
	public void setCursedWeaponEquipedId(int value)
	{
		_cursedWeaponEquipedId = value;
	}
	
	/**
	 * Gets the cursed weapon equiped id.
	 * @return the cursed weapon equiped id
	 */
	public int getCursedWeaponEquipedId()
	{
		return _cursedWeaponEquipedId;
	}
	
	/** The _charm of courage. */
	private boolean _charmOfCourage = false;
	
	/**
	 * Gets the charm of courage.
	 * @return the charm of courage
	 */
	public boolean getCharmOfCourage()
	{
		return _charmOfCourage;
	}
	
	/**
	 * Sets the charm of courage.
	 * @param val the new charm of courage
	 */
	public void setCharmOfCourage(boolean val)
	{
		_charmOfCourage = val;
		sendPacket(new EtcStatusUpdate(this));
	}
	
	/**
	 * Gets the death penalty buff level.
	 * @return the death penalty buff level
	 */
	public int getDeathPenaltyBuffLevel()
	{
		return _deathPenaltyBuffLevel;
	}
	
	/**
	 * Sets the death penalty buff level.
	 * @param level the new death penalty buff level
	 */
	public void setDeathPenaltyBuffLevel(int level)
	{
		_deathPenaltyBuffLevel = level;
	}
	
	/**
	 * Calculate death penalty buff level.
	 * @param killer the killer
	 */
	public void calculateDeathPenaltyBuffLevel(L2Character killer)
	{
		if ((Rnd.get(100) <= Config.DEATH_PENALTY_CHANCE) && !(killer instanceof L2PcInstance) && !(isGM()) && !(getCharmOfLuck() && ((killer instanceof L2BossInstance) || (killer instanceof L2RaidBossInstance))))
		{
			increaseDeathPenaltyBuffLevel();
		}
	}
	
	/**
	 * Increase death penalty buff level.
	 */
	public void increaseDeathPenaltyBuffLevel()
	{
		if (getDeathPenaltyBuffLevel() >= 15)
		{
			return;
		}
		
		if (getDeathPenaltyBuffLevel() != 0)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());
			
			if (skill != null)
			{
				removeSkill(skill, true);
			}
		}
		
		_deathPenaltyBuffLevel++;
		
		addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
		sendPacket(new EtcStatusUpdate(this));
		SystemMessage sm = new SystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED);
		sm.addNumber(getDeathPenaltyBuffLevel());
		sendPacket(sm);
	}
	
	/**
	 * Reduce death penalty buff level.
	 */
	public void reduceDeathPenaltyBuffLevel()
	{
		if (getDeathPenaltyBuffLevel() <= 0)
		{
			return;
		}
		
		L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());
		
		if (skill != null)
		{
			removeSkill(skill, true);
		}
		
		_deathPenaltyBuffLevel--;
		
		if (getDeathPenaltyBuffLevel() > 0)
		{
			addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
			sendPacket(new EtcStatusUpdate(this));
			SystemMessage sm = new SystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED);
			sm.addNumber(getDeathPenaltyBuffLevel());
			sendPacket(sm);
		}
		else
		{
			sendPacket(new EtcStatusUpdate(this));
			sendPacket(new SystemMessage(SystemMessageId.DEATH_PENALTY_LIFTED));
		}
	}
	
	/**
	 * Restore death penalty buff level.
	 */
	public void restoreDeathPenaltyBuffLevel()
	{
		L2Skill skill = SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel());
		
		if (skill != null)
		{
			removeSkill(skill, true);
		}
		
		if (getDeathPenaltyBuffLevel() > 0)
		{
			addSkill(SkillTable.getInstance().getInfo(5076, getDeathPenaltyBuffLevel()), false);
			// SystemMessage sm = new SystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED);
			// sm.addNumber(getDeathPenaltyBuffLevel());
			// sendPacket(sm);
		}
		// sendPacket(new EtcStatusUpdate(this));
	}
	
	/** The Reuse time stamps. */
	private final FastMap<Integer, TimeStamp> ReuseTimeStamps = new FastMap<Integer, TimeStamp>().shared();
	
	/**
	 * Simple class containing all neccessary information to maintain valid timestamps and reuse for skills upon relog. Filter this carefully as it becomes redundant to store reuse for small delays.
	 * @author Yesod
	 */
	private class TimeStamp
	{
		
		/** The skill. */
		private final int skill;
		
		/** The reuse. */
		private long reuse;
		
		/** The stamp. */
		private final Date stamp;
		
		/**
		 * Instantiates a new time stamp.
		 * @param _skill the _skill
		 * @param _reuse the _reuse
		 */
		public TimeStamp(int _skill, long _reuse)
		{
			skill = _skill;
			reuse = _reuse;
			stamp = new Date(new Date().getTime() + reuse);
		}
		
		/**
		 * Gets the skill.
		 * @return the skill
		 */
		public int getSkill()
		{
			return skill;
		}
		
		/**
		 * Gets the reuse.
		 * @return the reuse
		 */
		public long getReuse()
		{
			return reuse;
		}
		
		/*
		 * Check if the reuse delay has passed and if it has not then update the stored reuse time according to what is currently remaining on the delay.
		 */
		/**
		 * Checks for not passed.
		 * @return true, if successful
		 */
		public boolean hasNotPassed()
		{
			Date d = new Date();
			if (d.before(stamp))
			{
				reuse -= d.getTime() - (stamp.getTime() - reuse);
				return true;
			}
			return false;
		}
	}
	
	/**
	 * Index according to skill id the current timestamp of use.
	 * @param s the s
	 * @param r the r
	 */
	@Override
	public void addTimeStamp(int s, int r)
	{
		ReuseTimeStamps.put(s, new TimeStamp(s, r));
	}
	
	/**
	 * Index according to skill this TimeStamp instance for restoration purposes only.
	 * @param T the t
	 */
	private void addTimeStamp(TimeStamp T)
	{
		ReuseTimeStamps.put(T.getSkill(), T);
	}
	
	/**
	 * Index according to skill id the current timestamp of use.
	 * @param s the s
	 */
	@Override
	public void removeTimeStamp(int s)
	{
		ReuseTimeStamps.remove(s);
	}
	
	@Override
	public final void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		// Check if hit is missed
		if (miss)
		{
			sendPacket(new SystemMessage(SystemMessageId.MISSED_TARGET));
			return;
		}
		
		// Check if hit is critical
		if (pcrit)
		{
			sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT));
		}
		if (mcrit)
		{
			sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT_MAGIC));
		}
		
		SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DID_S1_DMG);
		sm.addNumber(damage);
		sendPacket(sm);
	}
	
	@Override
	public ForceBuff getForceBuff()
	{
		return _forceBuff;
	}
	
	@Override
	public void setForceBuff(ForceBuff fb)
	{
		_forceBuff = fb;
	}
}
