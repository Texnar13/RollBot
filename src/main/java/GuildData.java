import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;

// отслеживаемые сведения о конкретном канале
class GuildData {

    long guildId;
    long rollChannelId;

    int masterDice;


    // сведения о количестве единиц и двоек у пользователей кинувших кость в текущей сессии
    ArrayList<PlayerData> playersCurrentData = new ArrayList<>();


    GuildData(long guildId) {
        this.guildId = guildId;
    }

    PlayerData getPlayerByEvent(MessageReceivedEvent event) {
        // находим игрока по событию
        for (PlayerData playersCurrentDatum : playersCurrentData) {
            if (playersCurrentDatum.playerId == event.getAuthor().getIdLong()) {
                return playersCurrentDatum;
            }
        }
        return null;
    }

}
