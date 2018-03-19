package handlers.item;


import ru.catssoftware.gameserver.model.L2Object;
import ru.catssoftware.gameserver.model.L2World;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;
import ru.catssoftware.gameserver.model.quest.Quest;
import ru.catssoftware.gameserver.model.quest.QuestState;
import ru.catssoftware.gameserver.network.serverpackets.NpcHtmlMessage;
import ru.catssoftware.L2DatabaseFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

	/**
	* Author: DakChe,Elvis
	* Диалог npc для PersonalTeleport 
	*/


public class DialogTP extends Quest
{
	private static String		qn						= "DialogTP";

	public DialogTP()
	{
		super(-1, qn, "custom");

		int _npcId = 50017;
		addStartNpc(_npcId);
		addFirstTalkId(_npcId);
		addTalkId(_npcId);

	}

	@Override
	public String onFirstTalk(L2NpcInstance npc, L2PcInstance player)
	{
		return onTalk(npc,player);
	}
	
	@Override
	public String onTalk(L2NpcInstance npc, L2PcInstance talker)
	{	
		
		if (talker.getQuestState(qn)==null)
			newQuestState(talker);
		ViewTP(talker);
		return null;
	}
	
	@Override
	public String onEvent(String event, QuestState qs)
	{
		L2PcInstance player = qs.getPlayer();
		if (player == null)
			return null;
		String []commands = event.split(" ");
		if(commands[0].startsWith("TP"))
		{
			Connection con = null;
			try	//Вытаскиваем координаты из базы и телепортируем игрока к телепорту
			{
				con = L2DatabaseFactory.getInstance().getConnection(con);
				PreparedStatement statement = con.prepareStatement("SELECT X, Y, Z FROM Personal_Teleport WHERE charId = ?");
				statement.setInt(1, player.getCharId());
				ResultSet charTP = statement.executeQuery();
				while (charTP.next())
				{
					int X = charTP.getInt("X");			
					int Y = charTP.getInt("Y");
					int Z = charTP.getInt("Z");
					player.teleToLocation( X, Y, Z, true);
					player.sendMessage("Вы телепортированы");
				}

				charTP.close();
				statement.close();
			}
			catch (Exception e)
			{
				_log.fatal("DialogTP: Error reading X-Y-Z data from table: ", e);
			}
			
			DelNpcTP(player);
			
			return null;
		}

		return null;
	}

/* Метод удаления телепорта из мира и базы */

	private void DelNpcTP(L2PcInstance activeChar)
	{
		int _objectIdnpcTP = 0;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT NPC_ObjectId FROM Personal_Teleport WHERE charId=?");
			statement.setInt(1, activeChar.getCharId());
			ResultSet rset = statement.executeQuery();
			if (rset.next())
				_objectIdnpcTP = rset.getInt(1);
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.warn("", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
		
		L2Object obj = L2World.getInstance().findObject(_objectIdnpcTP);
		if ((obj != null) && (obj instanceof L2NpcInstance))
		{
			L2NpcInstance npcTP = (L2NpcInstance) obj;
			npcTP.deleteMe();
			con = null;
			try
				{	
					con = L2DatabaseFactory.getInstance().getConnection(con);
					PreparedStatement statement = con.prepareStatement("DELETE FROM Personal_Teleport WHERE charId = ?");
					statement.setInt(1, activeChar.getCharId());
					statement.execute();
					statement.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				finally
				{
					try
					{
						if (con != null)
							con.close();
					}
					catch (SQLException e)
					{
						e.printStackTrace();
					}
				}
			activeChar.sendMessage("Ваш телепорт исчез");
		}
		else
		{
			activeChar.sendMessage("Вы не имеете телепорт");
		}
	}
	
/*Проверка кол-ва телепортов в базе	*/

	public int currentPersonalTeleportCount(L2PcInstance activeChar)
	{
		int CountTP = 0;

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);
			PreparedStatement statement = con.prepareStatement("SELECT COUNT(charId) FROM Personal_Teleport WHERE charId=?");
			statement.setInt(1, activeChar.getCharId());
			ResultSet rset = statement.executeQuery();
			if (rset.next())
				CountTP = rset.getInt(1);
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.warn("", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
		return CountTP;
	}
	
	private void ViewTP(L2PcInstance activeChar)
	{	
		if (currentPersonalTeleportCount(activeChar) >= 1)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(activeChar.getObjectId());
				html.setFile("data/scripts/handlers/item/MAIN.htm");
				html.replace("%TP%", "<button width=80 height=20 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\" action=\"bypass -h Quest DialogTP TP\" value=\"Телепорт\">");
				activeChar.sendPacket(html);
			}
		else
			{
				NpcHtmlMessage html = new NpcHtmlMessage(activeChar.getObjectId());
				html.setFile("data/scripts/handlers/item/MAIN.htm");
				html.replace("%TP%", " ");
				activeChar.sendPacket(html);
			}
	}
		
	public static void main(String [] args) {
			new DialogTP();
	}
}
