import sys
from ru.catssoftware.gameserver import Announcements
from ru.catssoftware.gameserver import ThreadPoolManager
from ru.catssoftware.gameserver.model import L2World
from ru.catssoftware.gameserver.network.serverpackets import SystemMessage
from ru.catssoftware import L2DatabaseFactory
from java.lang import Runnable
 
	# Author: DakChe
	# Заводы: Запись в базу кол-во итемов на получение

 
# Ставим интервал в минутах.
INTERVAL = 6
 
class myTask( Runnable ): 
    def __init__( self ): 
        self.name = "RegenItem" 
    def run( self ): 
		UpdSumm1=L2DatabaseFactory.getInstance().getConnection()
		UpdSu1=UpdSumm1.prepareStatement("UPDATE TestPyton SET Item=Item+1 WHERE State='true'")
		try :
			UpdSu1.executeUpdate()
			UpdSu1.close()
			UpdSumm1.close()
		except :
			try : UpdSumm1.close()
			except : pass
		#Проверяем владельца
		Oldhost=L2DatabaseFactory.getInstance().getConnection()
		OldPlayer=Oldhost.prepareStatement("SELECT char_name FROM `TestPyton` WHERE State='true'")
		OldPl=OldPlayer.executeQuery()
		while (OldPl.next()) :
			OPN=OldPl.getString("char_name")
			try :
				OldPlayerName = OPN
				player = L2World.getInstance().getPlayer(OldPlayerName)
				if player:
					player.sendPacket(SystemMessage.sendString("Вам начислена прибыль с завода"))
			except :
				try :
					OldPlayer.close()
				except : pass
		try :
			Oldhost.close()
		except : pass

 
startInstance = myTask() 
ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(startInstance,INTERVAL*60000,INTERVAL*60000)