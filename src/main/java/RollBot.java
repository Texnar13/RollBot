import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class RollBot extends ListenerAdapter {
    // тег бота
    public static final String TAG = "RollBot";
    // todo сделать случайные приветствия!
    // todo стоит ограничение в 2000 символов, надо проверять и разбивать вывод на сообщения \о/

    // рандомайзер
    Random randomizer = new Random(System.currentTimeMillis());
    // время запуска бота
    static long startTimeMillis;

    // массив гильдий, в которых состоит бот
    static GuildData[] guilds;
    int currentGuildNumber;

    // читы
    int deadMasterDice = -1;
    int victoryMasterDice = -1;
    // читы привязываются к игроку, но не к серверу, тк передаются через личный чат
    List<PersonalCheatPoint> cheatPlayerList = new ArrayList<>();


    // метод запуска бота RollBot#0469
    public static void main(String[] args) {
        // builder аккаунта бота
        JDABuilder builder = JDABuilder.createDefault(TokenClass.BOT_TOKEN);
        // добавляем экземпляр этого класса в качестве обработчика сообщений
        builder.addEventListeners(new RollBot());

        // логинимся в дискорде
        try {// аккаунт бота
            builder.build();
            System.out.println(TAG + ":Log in success!");
        } catch (LoginException e) {
            System.out.println(TAG + ":Log in error!");
            e.printStackTrace();
            System.exit(1);
        }

        // считываем время начала работы бота
        startTimeMillis = System.currentTimeMillis();
    }

    // отработает 1 раз когда бот проснулся и подключился
    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        super.onReady(event);
        System.out.println(TAG + ":Connected!");

        // выставляем статус бота онлайн
        event.getJDA().getPresence().setStatus(OnlineStatus.ONLINE);
        event.getJDA().getPresence().setActivity(Activity.playing("DnD"));

        // получаем гильдии к которым подключен бот
        List<Guild> temp = event.getJDA().getGuilds();
        guilds = new GuildData[temp.size()];
        for (int i = 0; i < temp.size(); i++) {
            // загружаем данные оп одной гильдии(серверу)
            guilds[i] = getGuildDataFromFileByGuildId(temp.get(i).getIdLong());
        }
    }

    // метод отрабатывающий при получении сообщения
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        // получаем текст сообщения
        String msg = event.getMessage().getContentDisplay().trim();

        // выводим логи
        System.out.println("-----");// красивый разделитель
        if (event.isFromType(ChannelType.PRIVATE)) {
            // выводим сообщение в лог
            System.out.printf(TAG + ":[Private] %s: %s\n",
                    event.getAuthor().getName(),
                    msg
            );
        } else {
            System.out.printf(
                    TAG + ":[Server][%s][%s] %s(%s): %s\n",
                    event.getGuild().getName(), // гильдия (сервер)
                    event.getTextChannel().getName(), // канал
                    Objects.requireNonNull(event.getMember()).getEffectiveName(),// никнейм специфичный для данной гильдии написавшего
                    event.getAuthor().getName(), // имя написавшего
                    msg // сообщение
            );
        }

        // отфильтровываем сообщения от самого бота и пустые сообщения
        if (event.getAuthor().isBot()) return;
        if (msg.length() <= 2) return;
        if (msg.charAt(0) != '/' && msg.charAt(0) != '.') return;

        // todo сделать получение гильдии только в одном месте? getGuildFromListById
        if (event.isFromType(ChannelType.PRIVATE)) {// личное сообщение
            // номер текущей гильдии
            currentGuildNumber = -1;
            privateChatCommand(event, msg.substring(1));
        } else {// сообщение с сервера
            // номер текущей гильдии
            currentGuildNumber = getGuildNumberFromListById(event.getGuild().getIdLong());// todo проверка -1
            // обрабатываем
            serverChatCommand(event, msg.substring(1));
        }
    }

    // команды в личном чате
    private void privateChatCommand(MessageReceivedEvent event, String msg) {
        if (msg.startsWith("fun ")) {
            // можно писать в каналы веселые сообщения
            funCommand(event, msg);
            return;
        }
        if (msg.equals("help")) {
            // помощь
            event.getChannel().sendMessage(StringConstants.helpUTF8Message).queue();
            return;
        }

        try {// sendMessageInEventChannel(event, "Отключено!");
            if (msg.startsWith("dmd")) {
                // чит плохого
                deadMasterDice = Integer.parseInt(msg.substring(3).trim());
                sendMessageInEventChannel(event, "Везде не больше " + deadMasterDice + "...");
                return;
            }
            if (msg.startsWith("vmd")) {// чит хорошего
                victoryMasterDice = Integer.parseInt(msg.substring(3).trim());
                sendMessageInEventChannel(event, "Везде не меньше " + victoryMasterDice + "...");
                return;
            }
            if (msg.startsWith("mydmd")) {
                // чит персональный плохого
                int dmdCount = Integer.parseInt(msg.substring(5).trim());
                long playerId = event.getAuthor().getIdLong();

                PersonalCheatPoint point = getCheatPlayerById(playerId);

                if (point == null) {
                    point = new PersonalCheatPoint(playerId, dmdCount, -1);
                    cheatPlayerList.add(point);
                } else {
                    point.dmd = dmdCount;
                }

                sendMessageInEventChannel(event,
                        "Теперь у тебя не больше " + dmdCount + "..."
                );
                return;
            }
            if (msg.startsWith("myvmd")) {
                // чит персональный хорошего
                int vmdCount = Integer.parseInt(msg.substring(5).trim());
                long playerId = event.getAuthor().getIdLong();

                PersonalCheatPoint point = getCheatPlayerById(playerId);

                if (point == null) {
                    point = new PersonalCheatPoint(playerId, -1, vmdCount);
                    cheatPlayerList.add(point);
                } else {
                    point.vmd = vmdCount;
                }

                sendMessageInEventChannel(event, "Теперь у тебя не меньше " + vmdCount + "...");
                return;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            sendMessageInEventChannel(event, "Ошибка числа!");
        }

        if (msg.startsWith("myclear")) {
            // отключаем собственный чит
            cheatPlayerList.remove(getCheatPlayerById(event.getAuthor().getIdLong()));
            sendMessageInEventChannel(event, "my отключен ...");

            return;
        }

        if (msg.trim().equals("cheatinfo")) {
            // монитор читов
            StringBuilder str = new StringBuilder("dmd= ")
                    .append(((deadMasterDice < 0) ? ("Выключен") : (deadMasterDice)))
                    .append("\nvmd= ").append(((victoryMasterDice < 0) ? ("Выключен") : (victoryMasterDice)))
                    .append("\nигроки с персональными читами:\n");
            for (PersonalCheatPoint point : cheatPlayerList) {
                str.append(" - id=").append(point.playerId)
                        .append("  vmd=").append((point.vmd == -1) ? ("откл") : (point.vmd))
                        .append("  dmd=").append((point.dmd == -1) ? ("откл") : (point.dmd)).append('\n');
            }

            // помощь
            str.append('\n').append("Список команд(d главнее v, но на пересечениях учитываются оба):")
                    .append('\n').append("/dmd")
                    .append('\n').append("/vmd")
                    .append('\n').append("/mydmd")
                    .append('\n').append("/myvmd")
                    .append('\n').append("/myclear")
                    .append('\n').append("/cheatinfo");

            sendMessageInEventChannel(event, str.toString());
            return;
        }// todo получать имена игроков

        if (msg.equals("exit")) {
            // команда выхода
            exitCommand(event);
        }

        msg = clearRollFromFuckingRussianLetters(msg);
        if (msg.charAt(0) == 'd') {
            // кинуть кость
            rollCommand(event, msg);
        } else if (msg.charAt(0) == 'r') {
            // кинуть выражение
            rollCommand(event, msg.substring(1));
        }
    }

    // команды на сервере
    private void serverChatCommand(MessageReceivedEvent event, String msg) {

        // отфильтровываем сообщения только в нужном канале (если такая настройка стоит)
        if (guilds[currentGuildNumber].rollChannelId != -1)
            if (guilds[currentGuildNumber].rollChannelId != event.getChannel().getIdLong())// настройка есть, но канал не тот
                if (!msg.equals("bind"))
                    return;

        switch (msg) {
            case "help" ->// помощь
                    event.getChannel().sendMessage(StringConstants.serverHelpUTF8Message).queue();
            case "stat" ->// статистика
                    statisticsCommand(event, guilds[currentGuildNumber]);
            case "bind" -> {// смена кидальни
                // устанавливаем канал для работы бота на этом сервере
                setRollChannel(guilds[currentGuildNumber], event.getChannel().getIdLong());
                // говорим об этом пользователю
                sendMessageInEventChannel(event, "С этого момента кости кидаются только тут");
            }
            case "debug" ->// отладочный
                    debugCommand(event);
            case "exit" -> // команда выхода
                    exitCommand(event);


            // назначение главной кости
            default -> {
                if (msg.startsWith("md")) {
                    try {
                        guilds[currentGuildNumber].masterDice = Integer.parseInt(msg.substring(2).trim());

                        // сохраняем в файл
                        try {
                            // получаем данные из файла в json
                            JsonObject rootObject = getGuildJsonDataFromFileById(guilds[currentGuildNumber].guildId);

                            // меняем значения на новые
                            rootObject.addProperty("master_dice", Integer.toString(guilds[currentGuildNumber].masterDice));

                            // пишем json в файл
                            saveGuildGsonDataInFile(guilds[currentGuildNumber].guildId, rootObject);

                            sendMessageInEventChannel(event, "Теперь главная кость - " +
                                    guilds[currentGuildNumber].masterDice);

                        } catch (IOException e) {
                            sendMessageInEventChannel(event, "Ошибка сохранения!");
                            e.printStackTrace();
                        }

                    } catch (NumberFormatException e) {
                        sendMessageInEventChannel(event, "Ошибка числа!");
                    }
                }

                msg = clearRollFromFuckingRussianLetters(msg);
                if (msg.charAt(0) == 'd') {
                    // кинуть кость
                    rollCommand(event, msg);
                } else if (msg.charAt(0) == 'r') {
                    // кинуть выражение
                    rollCommand(event, msg.substring(1));
                }
            }
        }
    }


    private String clearRollFromFuckingRussianLetters(String rollString) {
        // получаем волшебные символы для проверок, из кодировки windows-1251
        // todo все исправил, просто убери этот код


        // переводим все в нижний регистр
        StringBuilder builder = new StringBuilder(rollString.toLowerCase());

        // меняем ошибки раскладки в выражении
        if (rollString.charAt(0) == 'к' || rollString.charAt(0) == 'р')
            builder.setCharAt(0, 'r');

        // все варианты обозначения кости
        for (int symbolI = 0; symbolI < builder.length(); symbolI++)
            if (builder.charAt(symbolI) == 'к' || builder.charAt(symbolI) == 'в' || builder.charAt(symbolI) == 'д')
                builder.setCharAt(symbolI, 'd');

        return builder.toString();
    }

    // ========================================= команды =========================================

    void statisticsCommand(MessageReceivedEvent event, GuildData guild) {

        // выводим время работы бота
        long millis = (System.currentTimeMillis() - startTimeMillis) % 1000;
        long second = ((System.currentTimeMillis() - startTimeMillis) / 1000) % 60;
        long minute = ((System.currentTimeMillis() - startTimeMillis) / (1000 * 60)) % 60;
        long hour = ((System.currentTimeMillis() - startTimeMillis) / (1000 * 60 * 60)) % 24;

        // прощаемся
        sendMessageInEventChannel(event,
                "Время работы: "
                        + String.format("%02dh %02dm %02d.%ds", hour, minute, second, millis)
        );

        // выводим статистику единиц и двадцаток
        ArrayList<PlayerData> playersData = guild.playersCurrentData;
        if (playersData.size() == 0) {
            sendMessageInEventChannel(event,
                    "За все время работы бота в этой гильдии, пока никто не выкинул ни одной единицы или двадцатки"
            );
        } else {
            // формируем статистику
            StringBuilder stat = new StringBuilder("Статистика для тех, кто онлайн:\nЗа эту игру:\n ======== Единицы: ======== \n");
            for (PlayerData playersDatum : playersData) {
                if (playersDatum.numberOfOnes > 0) {

                    // получаем имя игрока
                    if (event.getGuild().getMemberById(playersDatum.playerId) != null)
                        stat.append((Objects.requireNonNull(event.getGuild().getMemberById(playersDatum.playerId))).getEffectiveName())
                                .append(" - ").append(playersDatum.numberOfOnes).append('\n');
                }
            }
            stat.append("\n ======== Двадцатки: ======== \n");
            for (PlayerData playersDatum : playersData) {
                if (playersDatum.numberOfTwenties > 0) {
                    // получаем имя игрока
                    if (event.getGuild().getMemberById(playersDatum.playerId) != null)
                        stat.append(Objects.requireNonNull(event.getGuild().getMemberById(playersDatum.playerId)).getEffectiveName())
                                .append(" - ").append(playersDatum.numberOfTwenties).append('\n');
                }
            }


            stat.append("\n\n\nЗа все время:\n ======== Единицы: ======== \n");
            for (PlayerData playersDatum : playersData) {
                if (playersDatum.allNumberOfOnes > 0) {
                    // получаем имя игрока
                    if (event.getGuild().getMemberById(playersDatum.playerId) != null)
                        stat.append(Objects.requireNonNull(event.getGuild().getMemberById(playersDatum.playerId)).getEffectiveName())
                                .append(" - ").append(playersDatum.allNumberOfOnes).append('\n');
                }
            }
            stat.append("\n ======== Двадцатки: ======== \n");
            for (PlayerData playersDatum : playersData) {
                if (playersDatum.allNumberOfTwenties > 0) {
                    // получаем имя игрока
                    if (event.getGuild().getMemberById(playersDatum.playerId) != null)
                        stat.append(Objects.requireNonNull(event.getGuild().getMemberById(playersDatum.playerId)).getEffectiveName())
                                .append(" - ").append(playersDatum.allNumberOfTwenties).append('\n');
                }
            }

            // отправляем сообщение
            sendMessageInEventChannel(event, stat.toString());
        }
    }

    void debugCommand(MessageReceivedEvent event) {
        StringBuilder answer = new StringBuilder("Сервер: ")
                .append(event.getGuild().getName())
                .append(" (")
                .append(event.getGuild().getIdLong())
                .append(")\nУчастники онлайн:");

        List<Member> members = event.getGuild().getMembers();
        for (Member member : members) {
            answer.append("\n\t ")
                    .append(member.getEffectiveName())
                    .append(" (")
                    .append(member.getIdLong())
                    .append(')');
        }
        sendMessageInEventChannel(event, answer.toString());
    }

    void funCommand(MessageReceivedEvent event, String msg) {
        try {
            System.out.println(msg.substring(4, 22));
            System.out.println(msg.substring(23, 41));
            System.out.println(msg.substring(41));

            Objects.requireNonNull(
                    Objects.requireNonNull(
                            event.getJDA().getGuildById(Long.parseLong(msg.substring(4, 22)))
                    ).getTextChannelById(Long.parseLong(msg.substring(23, 41)))
            ).sendMessage(msg.substring(41)).queue();
        } catch (NullPointerException e) {
            sendMessageInEventChannel(event, "no such guild/channel error");
        } catch (NumberFormatException e) {
            sendMessageInEventChannel(event, "input numbers error");
        }
        // /fun 503170361903284245 689517268732084238 сообщение
        // (guild) roll 503170361903284245
        // (channel) кидальня 689517268732084238
        // для переноса строк нужно просто вводить enter
    }

    void rollCommand(MessageReceivedEvent event, String command) {

        Parser.RollAnswer answer;
        if (currentGuildNumber != -1) {
            // подгружаем главную кость
            int mainDice = guilds[currentGuildNumber].masterDice;

            // подгрузка читов
            int lowerThreshold = 1;
            int upperThreshold = mainDice;

            // проверяем на наличие глобальных
            if (1 <= victoryMasterDice && victoryMasterDice <= mainDice) {
                lowerThreshold = victoryMasterDice;
            }
            if (1 <= deadMasterDice && deadMasterDice <= mainDice) {
                upperThreshold = deadMasterDice;
            }
            // проверяем на наличие персональных читов
            PersonalCheatPoint point = getCheatPlayerById(event.getAuthor().getIdLong());
            if (point != null) {
                if (1 <= point.vmd && point.vmd <= mainDice) {
                    lowerThreshold = point.vmd;
                }
                if (1 <= point.dmd && point.dmd <= mainDice) {
                    upperThreshold = point.dmd;
                }
            }

            // парсим
            answer = Parser.parseRollString(
                    command,
                    mainDice,
                    randomizer,
                    lowerThreshold,
                    upperThreshold
            );
        } else {
            // парсим
            answer = Parser.parseRollString(
                    command,
                    200,
                    randomizer,
                    1,
                    200
            );
        }

        // выводим итоги
        switch (answer.errorPoz) {
            case -1:// без ошибок
                sendMessageInEventChannel(event,
                        "Rolled by " + getAuthorName(event) + ": " + answer.expression + "  =  " +
                                answer.number + "\n" + StringConstants.getBigTextInt(answer.number)
                );
                break;
            case -2:// в расчетах был деление на 0
                sendMessageInEventChannel(event,
                        "В общем в расчетах получилось деление на 0, а я так не умею. \nНо ты можешь попробовать еще раз :)"
                );
                break;
            case -3:// арифметическая ошибка
                sendMessageInEventChannel(event, "какая-то арифметическая ошибка, Ваня срочно смотри логи..");
                break;
            default:// ошибки
                sendMessageInEventChannel(event,
                        "/r " + command.replaceAll(" ", "").substring(0, answer.errorPoz) + "<= Здесь ошибка"
                );
        }

        // реакция бота и счетчик
        addReactionAndCounter(event, answer.numberOfOnes, answer.numberOfTwenties);
    }

    void exitCommand(MessageReceivedEvent event) {
        // выводим время работы бота
        long millis = (System.currentTimeMillis() - startTimeMillis) % 1000;
        long second = ((System.currentTimeMillis() - startTimeMillis) / 1000) % 60;
        long minute = ((System.currentTimeMillis() - startTimeMillis) / (1000 * 60)) % 60;
        long hour = ((System.currentTimeMillis() - startTimeMillis) / (1000 * 60 * 60)) % 24;

        // прощаемся
        sendMessageInEventChannel(event,
                "Bye, bye " + getAuthorName(event) + ".. Working time: "
                        + String.format("%02dh %02dm %02d.%ds", hour, minute, second, millis)
        );

        // задаем оффлайн статус
        event.getJDA().getPresence().setStatus(OnlineStatus.OFFLINE);

        // спим 2 секунды (чтобы все успело отправиться)
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Эта команда мягко говорит закрыться всем внутренним процессам дискорда,
        //  после закрытия сама программа будет работать еще секунд 10
        //  есть еще более агрессивная shutdownNow(), которая работает чуть быстрее
        //  и не рекомендую использовать System.exit(0);
        event.getJDA().shutdown();
    }

    // =========================================== кости ===========================================

    void addReactionAndCounter(MessageReceivedEvent event, int numberOfOnes, int numberOfTwenties) {

        // реакция бота и счетчик
        if (numberOfOnes > 0 || numberOfTwenties > 0) {

            // реакция бота
            if (numberOfOnes > 0) {
                event.getMessage().addReaction("\uD83E\uDD2F").queue();
            }
            if (numberOfTwenties > 0) {
                event.getMessage().addReaction("\uD83C\uDF87").queue();
            }

            // игрокам в гильдии начисляем статистику
            if (currentGuildNumber == -1) {
                // находим игрока кинувшего кости
                PlayerData currentPlayer = guilds[currentGuildNumber].getPlayerByEvent(event);

                // ищем в файле если он пуст или создаем нового
                if (currentPlayer == null) {
                    // создаем нового игрока
                    currentPlayer = new PlayerData();
                    currentPlayer.playerId = event.getAuthor().getIdLong();
                    guilds[currentGuildNumber].playersCurrentData.add(currentPlayer);
                    Point save = getPlayerPointsFromFileByIdOrCreateNew(event.getGuild().getIdLong(), event.getAuthor().getIdLong());
                    currentPlayer.allNumberOfOnes = save.numberOfOnes;
                    currentPlayer.allNumberOfTwenties = save.numberOfTwenties;
                }

                // считаем количество раз по 20 единиц и двадцаток до изменения (делением нацело)
                int twentyOnesCount = currentPlayer.allNumberOfOnes / 20;
                int twentyTwentiesCount = currentPlayer.allNumberOfTwenties / 20;


                // меняем данные игрока
                currentPlayer.numberOfOnes += numberOfOnes;
                currentPlayer.numberOfTwenties += numberOfTwenties;
                currentPlayer.allNumberOfOnes += numberOfOnes;
                currentPlayer.allNumberOfTwenties += numberOfTwenties;
                // добавляем данные так же и в файле
                addPointsInFileById(
                        event.getGuild().getIdLong(),
                        event.getAuthor().getIdLong(),
                        numberOfOnes,
                        numberOfTwenties
                );

                // считаем количество раз по 20 единиц и двадцаток после изменения (делением нацело)
                // для проверки на супер сообщение
                if (currentPlayer.allNumberOfOnes / 20 > twentyOnesCount) {
                    sendMessageInEventChannel(event, " ======== Уфф, это твоя двадцатая единица!========");
                }
                if (currentPlayer.allNumberOfTwenties / 20 > twentyTwentiesCount) {
                    sendMessageInEventChannel(event, " ======== Ура, это твоя двадцатая " + guilds[currentGuildNumber].masterDice + "! ======== ");
                }
            }
        }
    }

    // =================================== вспомогательные методы ===================================


    String getAuthorName(MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {// личное сообщение
            return event.getAuthor().getName();
        } else {// сообщение с сервера (обращаемся по нику а не по имени)
            return Objects.requireNonNull(event.getMember()).getEffectiveName();
        }
    }

    int getGuildNumberFromListById(long guildId) {
        // определяем текущую гильдию

        for (int guildI = 0; guildI < guilds.length; guildI++) {
            if (guilds[guildI].guildId == guildId) {
                return guildI;
            }
        }
        return -1;
    }

    // ========================================= хранилище =========================================

    Point getPlayerPointsFromFileByIdOrCreateNew(long guildId, long memberId) {
        try {
            // получаем данные из файла в json
            JsonObject rootObject = getGuildJsonDataFromFileById(guildId);
            JsonArray membersObject = rootObject.getAsJsonArray("members");

            // находим участника
            for (int i = 0; i < membersObject.size(); i++) {
                // получаем одного участника и проверяем не его ли это id
                if (membersObject.get(i).getAsJsonObject().get("_id").getAsLong() == memberId) {
                    // получаем ответ
                    return new Point(
                            membersObject.get(i).getAsJsonObject().get("number_of_ones").getAsInt(),
                            membersObject.get(i).getAsJsonObject().get("number_of_twenties").getAsInt()
                    );
                }
            }
            // игрока в сохраненных нет, возвращаем нули
            return new Point(0, 0);

        } catch (IllegalStateException | IOException e) {
            e.printStackTrace();
            // игрока в сохраненных нет, возвращаем нули
            return new Point(0, 0);
        }
    }

    GuildData getGuildDataFromFileByGuildId(long guildId) {
        /* пример структуры одного сервера:
        {
          "version_code":"1",
          "_id":"getGuild().getId()",
          "roll_channel":"event.getChannel().getIdLong()",    (канал, в котором можно кидать кости)
          "master_dice":"24",                                 (главная кость)
          "members":[
            {
              "_id":"getAuthor().getId()"
              "number_of_ones":1,
              "number_of_twenties":1
            }, ...
          ]
        }
        */

        // переданный id устанавливаем в созданный обьект гильдии
        GuildData guild = new GuildData(guildId);
        guild.masterDice = 20;
        try {
            // получаем данные из файла в json
            JsonObject rootObject = getGuildJsonDataFromFileById(guild.guildId);

            // получаем id канала для роллов (опционально)
            guild.rollChannelId = -1;
            try {
                guild.rollChannelId = rootObject.get("roll_channel").getAsLong();
            } catch (NumberFormatException e) {
                System.out.println(TAG + ": guild=" + guildId + " no roll_channel = " +
                        rootObject.get("roll_channel").getAsString());
            }

            // и главный куб
            try {
                guild.masterDice = rootObject.get("master_dice").getAsInt();
            } catch (NumberFormatException e) {
                System.out.println(TAG + ": guild=" + guildId + " no master_dice = " +
                        rootObject.get("master_dice").getAsString());
            }


            // получаем массив участников
            JsonArray membersObject = rootObject.getAsJsonArray("members");
            for (int i = 0; i < membersObject.size(); i++) {

                // получаем одного участника
                PlayerData player = new PlayerData();
                player.playerId = membersObject.get(i).getAsJsonObject().get("_id").getAsLong();
                player.allNumberOfOnes = membersObject.get(i).getAsJsonObject().get("number_of_ones").getAsInt();
                player.allNumberOfTwenties = membersObject.get(i).getAsJsonObject().get("number_of_twenties").getAsInt();
                guild.playersCurrentData.add(player);
            }

        } catch (IllegalStateException | IOException e) {
            e.printStackTrace();
            return null;
        }
        return guild;
    }

    // назначить гильдии канал для костей
    void setRollChannel(GuildData guild, long channelId) {
        // назначаем
        guild.rollChannelId = channelId;

        // сохраняем в файл
        try {
            // получаем данные из файла в json
            JsonObject rootObject = getGuildJsonDataFromFileById(guild.guildId);

            // меняем значения на новые
            rootObject.addProperty("roll_channel", Long.toString(guild.rollChannelId));

            // пишем json в файл
            saveGuildGsonDataInFile(guild.guildId, rootObject);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void addPointsInFileById(long guildId, long memberId, int appendNumberOfOnes, int appendNumberOfTwenties) {
        System.out.println(TAG + ":addPointsInFileById g:" + guildId + " p:" + memberId);

        try {
            // получаем данные из файла в json
            JsonObject rootObject = getGuildJsonDataFromFileById(guildId);
            JsonArray membersObject = rootObject.getAsJsonArray("members");

            // пополняем json новыми данными
            boolean writeFlag = false;
            for (int i = 0; i < membersObject.size() && !writeFlag; i++) {
                // получаем одного участника и проверяем не его ли это id
                if (membersObject.get(i).getAsJsonObject().get("_id").getAsLong() == memberId) {
                    // меняем значения на новые
                    membersObject.get(i).getAsJsonObject().addProperty(
                            "number_of_ones",
                            membersObject.get(i).getAsJsonObject().get("number_of_ones").getAsInt() + appendNumberOfOnes);
                    membersObject.get(i).getAsJsonObject().addProperty(
                            "number_of_twenties",
                            membersObject.get(i).getAsJsonObject().get("number_of_twenties").getAsInt() + appendNumberOfTwenties);
                    writeFlag = true;
                }
            }
            // создаем нового участника если необходимо
            if (!writeFlag) {
                JsonObject newMember = new JsonObject();
                newMember.addProperty("_id", memberId);
                newMember.addProperty("number_of_ones", appendNumberOfOnes);
                newMember.addProperty("number_of_twenties", appendNumberOfTwenties);

                membersObject.add(newMember);

            }

            // пишем json в файл
            saveGuildGsonDataInFile(guildId, rootObject);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // todo сохранять данные в сообщении мне
    // получаем json структуру из файла сервера, если структуры нет, создаем пустую json (без сохранения в файл)
    JsonObject getGuildJsonDataFromFileById(long guildId) throws IOException {

        // получаем папку с данными
        File rootDir = new File("__saved_data");
        if (rootDir.mkdir()) {
            System.out.println(TAG + ": Directory created");
        }
        // файл с данными конкретной гильдии
        File guild_data = new File(rootDir, guildId + ".txt");
        // если файла нет, создаем его
        if (guild_data.createNewFile()) {
            System.out.println(TAG + ": New file " + guildId + ".txt created!");
        }
        // читаем текст из файла сохраняя в строку
        FileReader reader = new FileReader(guild_data);
        StringBuilder contains = new StringBuilder();
        int c;
        while ((c = reader.read()) != -1) {
            contains.append((char) c);
        }
        reader.close();


        // пытаемся прочитать структуру json в файле
        JsonObject rootObject;
        try {
            // чтение главного объекта
            rootObject = JsonParser.parseString(contains.toString()).getAsJsonObject();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            // если структуры в файле нет, создаем новую со значениями по умолчанию
            System.out.println(TAG + ": create new json structure");
            rootObject = new JsonObject();
            rootObject.addProperty("version_code", "1");
            rootObject.addProperty("_id", Long.toString(guildId));
            rootObject.addProperty("roll_channel", "");
            rootObject.addProperty("master_dice", "20");
            rootObject.add("members", new JsonArray());
        }

        return rootObject;
    }

    // пишем json в файл (json должен быть подготовленным и не пустым)
    void saveGuildGsonDataInFile(long guildId, JsonObject rootObject) throws IOException {
        // получаем папку с данными
        File rootDir = new File("__saved_data");
        if (rootDir.mkdir()) {
            System.out.println(TAG + ": Directory created");
        }
        // файл с данными конкретной гильдии
        File guild_data = new File(rootDir, guildId + ".txt");
        // если файла нет, создаем его
        if (guild_data.createNewFile()) {
            System.out.println(TAG + ": New file " + guildId + ".txt created!");
        }
        // пишем json в файл
        String data = new Gson().toJson(rootObject);
        FileWriter writer = new FileWriter(guild_data);
        writer.append(data);
        writer.flush();

        // закрываем файл
        writer.close();
    }

    //    int getCheatPlayerPozById(long id) {
//        for (int i = 0; i < cheatPlayerList.size(); i++) {
//            if (cheatPlayerList.get(i).playerId == id) {
//                return i;
//            }
//        }
//        return -1;
//    }
    PersonalCheatPoint getCheatPlayerById(long id) {
        Iterator<PersonalCheatPoint> iterator = cheatPlayerList.iterator();
        while (iterator.hasNext()) {
            PersonalCheatPoint point = iterator.next();
            if (point.playerId == id) {
                return point;
            }
        }
        return null;
    }


    // ================= новое от 20.06.2021 ===========================================================================

    void sendMessageInEventChannel(MessageReceivedEvent event, String message) {
        event.getChannel().sendMessage(message).queue();
    }

}


// helpful code:
// event.getAuthor().getName() + event.getAuthor().getDiscriminator()
//event.getMessage().addReaction("\uD83C\uDFB2").queue();//":game_die:"


//      // достать данные
//      JsonElement jsonElement = JsonParser.parseString("{\"message\":\"Hi\",\"place\":{\"name\":\"World!\"}}");
//
//      JsonObject rootObject = jsonElement.getAsJsonObject(); // чтение главного объекта
//      String message = rootObject.get("message").getAsString(); // получить поле "message" как строку
//      JsonObject childObject = rootObject.getAsJsonObject("place"); // получить объект Place
//      String place = childObject.get("name").getAsString(); // получить поле "name"
//      System.out.println(message + " " + place); // напечатает "Hi World!"*/


//
//
//      // парсим данные в обьект
//      JsonObject rootObject = new JsonObject(); // создаем главный объект
//      rootObject.addProperty("message", "Hi"); // записываем текст в поле "message"
//      JsonObject childObject = new JsonObject(); // создаем объект Place
//      childObject.addProperty("name", "World!"); // записываем текст в поле "name" у объект Place
//      rootObject.add("place", childObject); // сохраняем дочерний объект в поле "place"
//
//      Gson gson = new Gson();
//      String json = gson.toJson(rootObject); // генерация json строки
//      System.out.println(json); // напечатает "{"message":"Hi","place":{"name":"World!"}}"

