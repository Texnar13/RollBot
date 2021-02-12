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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RollBot extends ListenerAdapter {
    // тег бота
    public static final String TAG = "RollBot";
    // текстовые цифры для вывода
    public static final String[][] NUMBERS_CODES = {
            {//0
                    "░███░",
                    "█░░░█",
                    "█░░░█",
                    "█░░░█",
                    "░███░"
            },
            {//1
                    "░░█░░",
                    "░██░░",
                    "░░█░░",
                    "░░█░░",
                    "░███░"
            },
            {//2
                    "░███░",
                    "█░░░█",
                    "░░░█░",
                    "░█░░░",
                    "█████"
            },
            {//3
                    "░███░",
                    "░░░░█",
                    "░░██░",
                    "░░░░█",
                    "░███░"
            },
            {//4
                    "█░░░█",
                    "█░░░█",
                    "█████",
                    "░░░░█",
                    "░░░░█"
            },
            {//5
                    "█████",
                    "█░░░░",
                    "████░",
                    "░░░░█",
                    "████░"
            },
            {//6
                    "░███░",
                    "█░░░░",
                    "████░",
                    "█░░░█",
                    "░███░"
            },
            {//7
                    "█████",
                    "░░░█░",
                    "░░█░░",
                    "░█░░░",
                    "░█░░░"
            },
            {//8
                    "░███░",
                    "█░░░█",
                    "░███░",
                    "█░░░█",
                    "░███░"
            },
            {//9
                    "░███░",
                    "█░░░█",
                    "░████",
                    "░░░░█",
                    "░███░"
            },
            {//-
                    "░░░░░",
                    "░░░░░",
                    "░███░",
                    "░░░░░",
                    "░░░░░"
            }
    };

    static final String helpUTF8Message = getUTF_8("Привет, я " + TAG +
            "\n Вот список доступных команд:" +
            "\n\t\t/dN (d10, d3, d100..) - кинуть кость и вывести получившееся значение (от 1 до N);" +
            "\n\t\t/r 2d2 * 2 + d2 - 5  - кинуть кости, посчитать выражение и вывести ответ" +
            "\n\t\t\t(Поддерживаются + * / - dN KdN. Скобки пока не поддерживаются!);" +
            "\n\t\t/help - показ этого сообщения;" +
            "\n\t\t/exit - завершение работы бота;");

    static final String serverHelpUTF8Message = getUTF_8("Привет, я " + TAG +
            "\n Вот список доступных команд:" +
            "\n\t\t/dN (d10, d3, d100..) - кинуть кость и вывести получившееся значение (от 1 до N);" +
            "\n\t\t/r 2d2 * 2 + d2 - 5  - кинуть кости, посчитать выражение и вывести ответ" +
            "\n\t\t\t(Поддерживаются + * / - dN KdN. Скобки пока не поддерживаются!);" +
            "\n\t\t/stat - количество двадцаток и единиц за текущую игру;" +
            "\n\t\t/help - показ этого сообщения;" +
            "\n\t\t/bind - назначить канал для костей, писать в нужном канале;" +
            "\n\t\t/exit - завершение работы бота;");

    static final String inputErrorMessage = getUTF_8("Вы ввели некорректное выражение для броска! Читайте /help");

    // рандомайзер
    Random r = new Random(System.currentTimeMillis());
    // врремя запуска бота
    static long startTimeMillis;

    // массив гильдий, в которых состоит бот
    static GuildData[] guilds;
    int currentGuildNumber;


    // читы
    int deadMasterDice = -1;


    // метод запуска бота
    public static void main(String[] args) {//RollBot#0469 ▼►▲◄

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

        //newGetRollAnswer("(5+(5+5))");

    }

    // метод отрабатывающий при получении сообщения
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        // получаем текст сообщения
        String msg = getUNICODE(event.getMessage().getContentDisplay());

        // выводим логи
        System.out.println("-----");// красивый разделитель
        if (event.isFromType(ChannelType.PRIVATE)) {
            // выводим сообщение в лог
            System.out.printf(TAG + ":[Private] %s: %s\n",
                    getUNICODE(event.getAuthor().getName()),
                    msg
            );
        } else {
            System.out.printf(
                    TAG + ":[Server][%s][%s] %s(%s): %s\n",
                    getUNICODE(event.getGuild().getName()), // гильдия (сервер)
                    getUNICODE(event.getTextChannel().getName()), // канал
                    getUNICODE(Objects.requireNonNull(event.getMember()).getEffectiveName()),// никнейм специфичный для данной гильдии написавшего
                    getUNICODE(event.getAuthor().getName()), // имя написавшего
                    msg // сообщение
            );
        }


        //todo сделать получение гильдии только в одном месте? getGuildFromListById

        // отфильтровываем сообщения от самого бота и пустые сообщения
        if (event.getAuthor().isBot() || msg.length() == 0) return;

        // ================================== личное сообщение ==================================
        if (event.isFromType(ChannelType.PRIVATE)) {


            // номер текущей гильдии
            currentGuildNumber = -1;

            if (msg.startsWith("/fun ")) {
                // можно писать в каналы веселые сообщения
                funCommand(event, msg);
                return;
            } else if (msg.equals("/help")) {
                // помощь
                event.getChannel().sendMessage(helpUTF8Message).queue();
                return;
            } else if (msg.startsWith("/dmd")) {
                // чит плохого
                //try {
                //    deadMasterDice = Integer.parseInt(msg.substring(4).trim());
                //    event.getChannel().sendMessage(getUTF_8("не больше " + deadMasterDice + "...")).queue();
                //} catch (NumberFormatException e) {
                //    event.getChannel().sendMessage(getUTF_8("Ошибка числа!")).queue();
                //}
                event.getChannel().sendMessage(getUTF_8("Отключено!")).queue();
                return;
            } else if (msg.trim().equals("/cheatinfo")) {
                // монитор читов
                event.getChannel().sendMessage(getUTF_8(
                        "dmd= " + ((deadMasterDice == -1) ? ("Выключен") : (deadMasterDice))
                )).queue();
                return;
            }

        } else {
            // ================================== сообщение с сервера ==================================


            // номер текущей гильдии
            currentGuildNumber = getGuildNumberFromListById(event.getGuild().getIdLong());// todo проверка -1


            if (msg.charAt(0) == '/') {

                // отфильтровываем сообщения только в нужном канале (если такая настройка стоит)
                if (guilds[currentGuildNumber].rollChannelId != -1)
                    if (guilds[currentGuildNumber].rollChannelId != event.getChannel().getIdLong())// настройка есть, но канал не тот
                        if (!msg.equals("/bind")) return;

                switch (msg) {
                    // помощь
                    case "/help":
                        event.getChannel().sendMessage(serverHelpUTF8Message).queue();
                        break;

                    // статистика
                    case "/stat":
                        statisticsCommand(event, guilds[currentGuildNumber]);
                        break;

                    // смена кидальни
                    case "/bind":
                        // устанавливаем канал для работы бота на этом сервере
                        setRollChannel(guilds[currentGuildNumber], event.getChannel().getIdLong());
                        // говорим об этом пользователю
                        event.getChannel().sendMessage(getUTF_8("С этого момента кости кидаются только тут")).queue();
                        break;

                    // отладочный
                    case "/debug":
                        debugCommand(event);
                        break;

                    // назначение главной кости
                    default: {
                        if (msg.startsWith("/md")) {
                            try {
                                guilds[currentGuildNumber].masterDice = Integer.parseInt(msg.substring(3));
                            } catch (NumberFormatException e) {
                                event.getChannel().sendMessage(getUTF_8("Ошибка числа!")).queue();
                            }
                        }
                    }
                }
            }
        }

        // ================================== команда бота для всех чатов ==================================
        if (msg.charAt(0) == '/') {

            if (msg.charAt(1) == 'd' || msg.charAt(1) == 'D') {
                // кинуть кость
                rollCommand(event, msg.substring(1));
            } else if (msg.charAt(1) == 'r' || msg.charAt(1) == 'R') {
                // кинуть выражение
                rollCommand(event, msg.substring(2));
            } else if (msg.equals("/exit")) {
                // команда выхода
                exitCommand(event);
            }
        }
    }

    // ========================================= комманды =========================================

    void statisticsCommand(MessageReceivedEvent event, GuildData guild) {
        // выводим статистику единиц и двадцаток
        ArrayList<PlayerData> playersData = guild.playersCurrentData;
        if (playersData.size() == 0) {
            event.getChannel().sendMessage(getUTF_8(
                    "За все время работы бота в этой гильдии, пока никто не выкинул ни одной единицы или двадцатки"
            )).queue();
        } else {
            // формиируем статистику
            StringBuilder stat = new StringBuilder("Статистика для тех, кто онлайн:\nЗа эту игру:\n ======== Единицы: ======== \n");
            for (PlayerData playersDatum : playersData) {
                if (playersDatum.numberOfOnes > 0) {

                    // получаем имя игрока
                    if (event.getGuild().getMemberById(playersDatum.playerId) != null)
                        stat.append(getUNICODE((Objects.requireNonNull(event.getGuild().getMemberById(playersDatum.playerId))).getEffectiveName()))
                                .append(" - ").append(playersDatum.numberOfOnes).append('\n');
                }
            }
            stat.append("\n ======== Двадцатки: ======== \n");
            for (PlayerData playersDatum : playersData) {
                if (playersDatum.numberOfTwenties > 0) {
                    // получаем имя игрока
                    if (event.getGuild().getMemberById(playersDatum.playerId) != null)
                        stat.append(getUNICODE(Objects.requireNonNull(event.getGuild().getMemberById(playersDatum.playerId)).getEffectiveName()))
                                .append(" - ").append(playersDatum.numberOfTwenties).append('\n');
                }
            }


            stat.append("\n\n\nЗа все время:\n ======== Единицы: ======== \n");
            for (PlayerData playersDatum : playersData) {
                if (playersDatum.allNumberOfOnes > 0) {
                    // получаем имя игрока
                    if (event.getGuild().getMemberById(playersDatum.playerId) != null)
                        stat.append(getUNICODE(Objects.requireNonNull(event.getGuild().getMemberById(playersDatum.playerId)).getEffectiveName()))
                                .append(" - ").append(playersDatum.allNumberOfOnes).append('\n');
                }
            }
            stat.append("\n ======== Двадцатки: ======== \n");
            for (PlayerData playersDatum : playersData) {
                if (playersDatum.allNumberOfTwenties > 0) {
                    // получаем имя игрока
                    if (event.getGuild().getMemberById(playersDatum.playerId) != null)
                        stat.append(getUNICODE(Objects.requireNonNull(event.getGuild().getMemberById(playersDatum.playerId)).getEffectiveName()))
                                .append(" - ").append(playersDatum.allNumberOfTwenties).append('\n');
                }
            }

            // отправляем сообщение
            event.getChannel().sendMessage(getUTF_8(stat.toString())).queue();
        }
    }

    void debugCommand(MessageReceivedEvent event) {
        StringBuilder answer = new StringBuilder("Сервер: ")
                .append(getUNICODE(event.getGuild().getName()))
                .append(" (")
                .append(event.getGuild().getIdLong())
                .append(")\nУчастники онлайн:");

        List<Member> members = event.getGuild().getMembers();
        for (Member member : members) {
            answer.append("\n\t ")
                    .append(getUNICODE(member.getEffectiveName()))
                    .append(" (")
                    .append(member.getIdLong())
                    .append(')');
        }
        event.getChannel().sendMessage(getUTF_8(answer.toString())).queue();
    }

    void funCommand(MessageReceivedEvent event, String msg) {
        try {
            Objects.requireNonNull(
                    Objects.requireNonNull(
                            event.getJDA().getGuildById(Long.parseLong(msg.substring(5, 23)))
                    ).getTextChannelById(Long.parseLong(msg.substring(24, 42)))
            ).sendMessage(getUTF_8(msg.substring(42))).queue();
        } catch (NullPointerException e) {
            event.getChannel().sendMessage(getUTF_8("no such guild/channel error")).queue();
        } catch (java.lang.NumberFormatException e) {
            event.getChannel().sendMessage(getUTF_8("input numbers error")).queue();
        }
        // /fun 503170361903284245 689517268732084238 сообщение
        // (guild) roll 503170361903284245
        // (channel) кидальня 689517268732084238
        // для переноса строк нужно просто вводить enter
    }

    void rollCommand(MessageReceivedEvent event, String command) {
        // проверка на валидные значения
        try {

            int mainDice = 20;
            if (currentGuildNumber != -1) {
                mainDice = guilds[currentGuildNumber].masterDice;
            }
            RollAnswer answer = newGetRollAnswer(
                    mainDice,
                    command.replaceAll(" ", "")// удаляем пробелы
            );
            System.out.println("+++++++++++e=" + answer.errorPoz);

            if (answer.errorPoz == -1) {
                event.getChannel().sendMessage(getUTF_8(
                        "Rolled by " + getAuthorName(event) + ":" + answer.expression + "  =  " +
                                answer.number + "\n" + getTextInt(answer.number)
                )).queue();
            } else {
                event.getChannel().sendMessage(getUTF_8(
                        "/r " + command.replaceAll(" ", "").substring(0, answer.errorPoz) + "<= Здесь ошибка"
                )).queue();
            }

            // реакция бота и счетчик
            addCounterAndReaction(event, answer.numberOfOnes, answer.numberOfTwenties);

        } catch (java.lang.NumberFormatException e) {
            event.getChannel().sendMessage(inputErrorMessage).queue();
        }
    }

    void exitCommand(MessageReceivedEvent event) {
        // выводим время работы бота
        long millis = (System.currentTimeMillis() - startTimeMillis) % 1000;
        long second = ((System.currentTimeMillis() - startTimeMillis) / 1000) % 60;
        long minute = ((System.currentTimeMillis() - startTimeMillis) / (1000 * 60)) % 60;
        long hour = ((System.currentTimeMillis() - startTimeMillis) / (1000 * 60 * 60)) % 24;

        // прощаемся
        event.getChannel().sendMessage(getUTF_8(
                "Bye, bye " + getAuthorName(event) + ".. Working time: "
                        + String.format("%02dh %02dm %02d.%ds", hour, minute, second, millis)
        )).queue();

        // задаем оффлайн статус
        event.getJDA().getPresence().setStatus(OnlineStatus.OFFLINE);

        // спим 2 секунды (чтобы все успело отправиться)
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    // =========================================== кости ===========================================

    int roll(int n) {
        if (n <= 0) throw new java.lang.NumberFormatException();
        // чит плохой кости
        if (deadMasterDice != -1 && mainDice == n && deadMasterDice < mainDice) {
            n = deadMasterDice;
        }
        return (r.nextInt(n)) + 1;
    }

    RollAnswer newGetRollAnswer(int mainDice, String rollS) {
//        return getRollAnswer(rollS);
        tempRollS = rollS + "";
        this.mainDice = mainDice;
        return sumCheck();
    }


    // отладка
    String append = "";

    // временная строка хранящая непрочтенные символы
    String tempRollS;

    // текущая главная кость
    int mainDice;

    // ищем выражение в скобках или кость/число в строке
    RollAnswer bracketsCheck() {
        RollAnswer r = new RollAnswer();

        System.out.println(append + "bracketsCheck_" + tempRollS);

        if (tempRollS.length() != 0) {
            // конструкция в скобках
            if (tempRollS.charAt(0) == '(') {
                append = append + ' ';

                // нашли вторую скобку, считаем выражение внутри этих двух скобок
                tempRollS = tempRollS.substring(1);
                RollAnswer tAns = sumCheck();
                System.out.println(append + "1_" + tempRollS);

                // предыдущий метод должен был дойти до закрывающей скобки
                if (tempRollS.charAt(0) == ')') {
                    tempRollS = tempRollS.substring(1);
                    // суммируем это с основным выражением
                    r.expression.append("(").append(tAns.expression).append(")");
                    r.number += tAns.number;
                    r.numberOfOnes += tAns.numberOfOnes;
                    r.numberOfTwenties += tAns.numberOfTwenties;
                    r.checkedCorrectLength += tAns.checkedCorrectLength + 1;
                    if (tAns.errorPoz != -1) {
                        r.errorPoz = tAns.errorPoz + tAns.checkedCorrectLength;
                    } else
                        r.errorPoz = tAns.errorPoz;
                    append = append.substring(0, append.length() - 1);
                } else {
                    // если закрывающей скобки нет, это ошибка
                    r.errorPoz = tAns.checkedCorrectLength;
                    System.out.println(append + "errr");
                }
            } else {// просто множитель
                r = rollCheck();
                System.out.println(append + "2_" + tempRollS);
            }
        } else {// просто множитель
            r = rollCheck();
            System.out.println(append + "3_" + tempRollS);
        }
        return r;
    }

    // ищем слагаемые в строке
    RollAnswer sumCheck() {//r (5+(5+5))
        System.out.println(append + "addCheck_s_ " + tempRollS);


        // сразу отдаем на проверку
        RollAnswer r = multiplyCheck();
        System.out.println(append + "addCheck_af_ " + tempRollS + " len=" + r.checkedCorrectLength + " err=" + r.errorPoz);


        while (tempRollS.length() != 0 && r.errorPoz == -1) {
            if ((tempRollS.charAt(0) != '+' && tempRollS.charAt(0) != '-')) {
                break;
            }

            char first = tempRollS.charAt(0);

            tempRollS = tempRollS.substring(1);
            RollAnswer tAns = multiplyCheck();
            System.out.println(append + "addCheck_as_ " + tempRollS + " len=" + r.checkedCorrectLength + " lent=" + tAns.checkedCorrectLength + " err=" + r.errorPoz + " errt=" + tAns.errorPoz);

            // суммируем это с основным выражением
            if (first == '+') {
                r.expression.append('+');
                r.number += tAns.number;
            } else {
                r.expression.append('-');
                r.number -= tAns.number;
            }
            r.expression.append(tAns.expression);
            r.numberOfOnes += tAns.numberOfOnes;
            r.numberOfTwenties += tAns.numberOfTwenties;
            r.checkedCorrectLength += tAns.checkedCorrectLength + 1;
            if (tAns.errorPoz != -1) {
                r.errorPoz = tAns.errorPoz + r.checkedCorrectLength;
            } else {
                r.errorPoz = tAns.errorPoz;
            }
        }
        return r;
    }

    // ищем множители в строке
    RollAnswer multiplyCheck() {
        System.out.println(append + "multiplyCheck_s_ " + tempRollS);

        // сразу отдаем на проверку
        RollAnswer r = bracketsCheck();
        System.out.println(append + "multiplyCheck_af_ " + tempRollS + " len=" + r.checkedCorrectLength + " err=" + r.errorPoz);


        while (tempRollS.length() != 0 && r.errorPoz == -1) {
            if ((tempRollS.charAt(0) != '*' && tempRollS.charAt(0) != '/')) {
                break;
            }
            System.out.println("12345");

            char first = tempRollS.charAt(0);

            tempRollS = tempRollS.substring(1);
            RollAnswer tAns = bracketsCheck();
            System.out.println(append + "multiplyCheck_as_ " + tempRollS + " len=" + r.checkedCorrectLength + " lent=" + tAns.checkedCorrectLength + " err=" + r.errorPoz + " errt=" + tAns.errorPoz);

            // суммируем это с основным выражением
            if (first == '*') {
                r.expression.append(" * ");
                r.number *= tAns.number;
            } else {
                r.expression.append(" / ");
                r.number /= tAns.number;
            }
            r.expression.append(tAns.expression);
            r.numberOfOnes += tAns.numberOfOnes;
            r.numberOfTwenties += tAns.numberOfTwenties;
            r.checkedCorrectLength += (tAns.checkedCorrectLength + 1);
            if (tAns.errorPoz != -1) {
                r.errorPoz = tAns.errorPoz + r.checkedCorrectLength;
            } else {
                r.errorPoz = tAns.errorPoz;
            }
        }
        return r;
    }

    // считываем кость/число из строки
    RollAnswer rollCheck() {
        // 55
        // -55
        // 55d55

        System.out.println("rollCheck " + tempRollS);

        RollAnswer r = new RollAnswer();
        if (tempRollS.length() == 0) {
            r.errorPoz = 0;
            return r;
        }


        boolean minus = false;
        int startNumber = -1;
        int diceNumber = -1;


        // считываем минус в начале числа, если он есть
        if (tempRollS.charAt(0) == '-') {
            minus = true;
            tempRollS = tempRollS.substring(1);
        }

        int charPoz = 0;
        // считываем первую часть кости или число
        while (tempRollS.charAt(charPoz) == '0' ||
                tempRollS.charAt(charPoz) == '1' ||
                tempRollS.charAt(charPoz) == '2' ||
                tempRollS.charAt(charPoz) == '3' ||
                tempRollS.charAt(charPoz) == '4' ||
                tempRollS.charAt(charPoz) == '5' ||
                tempRollS.charAt(charPoz) == '6' ||
                tempRollS.charAt(charPoz) == '7' ||
                tempRollS.charAt(charPoz) == '8' ||
                tempRollS.charAt(charPoz) == '9'
        ) {
            charPoz++;
            // проверка конца строки
            if (tempRollS.length() == charPoz) {
                break;
            }
        }
        // если число пустое
        if (charPoz == 0) {
            startNumber = 0;
        } else {
            try {
                startNumber = Integer.parseInt(tempRollS.substring(0, charPoz));
            } catch (NumberFormatException e) {
                r.errorPoz = 0;
                System.out.println("1err " + tempRollS.substring(0, charPoz));
            }
        }

        // если не конец строки
        if (tempRollS.length() != charPoz) {
            // считываем часть д
            if (tempRollS.charAt(charPoz) == 'd') {
                charPoz++;
                int startDicePoz = charPoz;
                // если не конец строки
                if (tempRollS.length() != charPoz) {
                    while (tempRollS.charAt(charPoz) == '0' ||
                            tempRollS.charAt(charPoz) == '1' ||
                            tempRollS.charAt(charPoz) == '2' ||
                            tempRollS.charAt(charPoz) == '3' ||
                            tempRollS.charAt(charPoz) == '4' ||
                            tempRollS.charAt(charPoz) == '5' ||
                            tempRollS.charAt(charPoz) == '6' ||
                            tempRollS.charAt(charPoz) == '7' ||
                            tempRollS.charAt(charPoz) == '8' ||
                            tempRollS.charAt(charPoz) == '9'
                    ) {
                        charPoz++;
                        // проверка конца строки
                        if (tempRollS.length() == charPoz) {
                            break;
                        }
                    }

                    // число после d пустое, ошибка
                    if (startDicePoz - charPoz == 0) {
                        r.errorPoz = startDicePoz;
                        System.out.println("2err");
                    } else if (tempRollS.length() != charPoz) {
                        // если это не конец строки и дальше стоят неправильные символы
                        if (tempRollS.charAt(charPoz) != '*' && tempRollS.charAt(charPoz) != '/' &&
                                tempRollS.charAt(charPoz) != '+' && tempRollS.charAt(charPoz) != '-' &&
                                tempRollS.charAt(charPoz) != ')'
                        ) {
                            r.errorPoz = charPoz;
                            System.out.println("3err");
                        } else {// все правильно
                            try {
                                diceNumber = Integer.parseInt(tempRollS.substring(startDicePoz, charPoz));
                            } catch (NumberFormatException e) {
                                r.errorPoz = startDicePoz;
                                System.out.println("4err");
                            }
                        }
                    } else {// все правильно
                        try {
                            diceNumber = Integer.parseInt(tempRollS.substring(startDicePoz, charPoz));
                        } catch (NumberFormatException e) {
                            r.errorPoz = startDicePoz;
                            System.out.println("5err");
                        }
                    }
                }
            }
        }
        //        String diceValue = tempRollS.substring(0, charPoz);

        // если есть число кости, то кидаем ее
        if (diceNumber != -1) {


            // есть ли коэффициэнт перед костью
            if (startNumber == 0 || startNumber == 1) {// бросаем простую кость
                int roll = roll(diceNumber);

                if (minus) {
                    r.number = -roll;
                    r.expression = new StringBuilder("-<").append(roll).append(">");
                } else {
                    r.number = roll;
                    r.expression = new StringBuilder("<").append(roll).append(">");
                }
                // на двадцатых костях считаем особые числа
                if (diceNumber == 20 && roll == 1) r.numberOfOnes++;
                if (diceNumber == 20 && roll == mainDice) r.numberOfTwenties++;

            } else {// бросаем кость несколько раз
                // кидаем кости и считаем результат
                if (minus) {
                    r.expression.append("-(");
                } else {
                    r.expression.append("(");
                }

                for (int i = 0; i < startNumber; i++) {
                    // кидаем одну из костей
                    int roll = roll(diceNumber);
                    // добавляем все в красивую строку
                    r.expression.append("<").append(roll).append(">");
                    if (i != startNumber - 1) r.expression.append('+');
                    // добавляем то что выпало
                    if (minus) {
                        r.number -= roll;
                    } else {
                        r.number += roll;
                    }
                    // если кидали 20ку и выпали особые добавляем их к счетчику в ответе
                    if (diceNumber == 20 && roll == 1) r.numberOfOnes++;
                    if (diceNumber == 20 && roll == mainDice) r.numberOfTwenties++;
                }
                r.expression.append(")");
            }

        } else {// простое число
            if (minus) {
                System.out.println(startNumber + " 1234567890");
                r.number = -startNumber;
                r.expression = new StringBuilder().append('-').append(startNumber);
            } else {
                r.number = startNumber;
                r.expression = new StringBuilder().append(startNumber);
            }
        }

        tempRollS = tempRollS.substring(charPoz);
        System.out.println("d => " + tempRollS + " r.number=" + r.number + " m =" + minus);
        r.checkedCorrectLength = charPoz;
        return r;

    }




    /*
     * todo ошибки
     *
     *  /rd 0+  =>  Вы ввели некорректное выражение для броска! Читайте /help
     *
     *  /r 0+   => err = 0
     * -----
     * RollBot:[Private] Texnar13: /r 0+
     * addCheck_s_ 0+
     * multiplyCheck_s_ 0+
     * bracketsCheck_0+
     * d  =>+
     * 2_+
     * multiplyCheck_af_ + len=0 err=-1
     * addCheck_af_ + len=0 err=-1
     * multiplyCheck_s_
     * bracketsCheck_
     * 3_
     * multiplyCheck_af_  len=0 err=0
     * addCheck_as_  len=0 lent=0 err=-1 errt=0
     * +++++++++++e=1
     * -----
     *
     *
     *
     *
     *
     *
     *  /r--1
     *  Texnar13:-0-1  =  -1
     *
     * */


//    RollAnswer getRollAnswer(String rollS) {
//
//        if (rollS.contains("+")) {// разбиваем выражение на слагаемые и передаем дальше
//            // разбиваем строку на отдельные суммируемые части
//            String[] expressions = rollS.split("\\+");
//
//            // высчитываем каждую часть и суммируем все
//            RollAnswer answer = new RollAnswer();
//            answer.expression.append("( ");
//            for (int i = 0; i < expressions.length - 1; i++) {
//                // считаем конкретную часть
//                RollAnswer ans = getRollAnswer(expressions[i]);
//                // суммируем ее с остальными
//                answer.expression.append(ans.expression).append(" + ");
//                answer.number += ans.number;
//                answer.numberOfOnes += ans.numberOfOnes;
//                answer.numberOfTwenties += ans.numberOfTwenties;
//            }
//            RollAnswer ans = getRollAnswer(expressions[expressions.length - 1]);
//            answer.expression.append(ans.expression).append(" )");
//            answer.number += ans.number;
//
//            // возвращаем полученный результат
//            return answer;
//
//        } else if (rollS.contains("-")) {// разбиваем выражение на вычитаемые и передаем дальше
//            // разбиваем строку на отдельные вычитаемые части
//            String[] expressions = rollS.split("[-]");
//
//            // высчитываем каждую часть и суммируем все
//            RollAnswer answer = new RollAnswer();
//            answer.expression.append("( ");
//            // если перед первым операндом не стоит отрицательный знак
//            if (expressions[0].length() != 0) {
//                RollAnswer ans = getRollAnswer(expressions[0]);
//                answer.expression.append(ans.expression).append(" - ");
//                answer.number += ans.number;
//                answer.numberOfOnes += ans.numberOfOnes;
//                answer.numberOfTwenties += ans.numberOfTwenties;
//            } else {
//                answer.expression.append('-');
//            }
//
//            // вычитаем обычные части
//            for (int i = 1; i < expressions.length; i++) {
//                RollAnswer ans = getRollAnswer(expressions[i]);
//                answer.expression.append(ans.expression);
//                if (i == expressions.length - 1) {
//                    answer.expression.append(" )");
//                } else {
//                    answer.expression.append(" - ");
//                }
//                answer.number -= ans.number;
//                answer.numberOfOnes += ans.numberOfOnes;
//                answer.numberOfTwenties += ans.numberOfTwenties;
//            }
//
//            // возвращаем полученный результат
//            return answer;
//
//        } else if (rollS.contains("*") || rollS.contains("/")) {// разбиваем выражение на множители и передаем дальше
//
//            // создаем ответ
//            RollAnswer answer = new RollAnswer();
//            answer.number = 1;
//
//            // пробегаемся по строке ища знаки или конец строки
//            answer.expression.append("( ");
//            int previousPoz = -1;
//            for (int i = 1; i < rollS.length(); i++) {
//                if (rollS.charAt(i) == '*' || rollS.charAt(i) == '/' || i == rollS.length() - 1) {
//                    // считаем отдельную часть
//                    RollAnswer ans;
//                    if (i == rollS.length() - 1) {
//                        ans = getRollAnswer(rollS.substring(previousPoz + 1));
//                    } else {
//                        ans = getRollAnswer(rollS.substring(previousPoz + 1, i));
//                    }
//
//                    // записываем посчитанное в общую переменную
//                    answer.numberOfOnes += ans.numberOfOnes;
//                    answer.numberOfTwenties += ans.numberOfTwenties;
//                    if (previousPoz != -1) {
//                        if (rollS.charAt(previousPoz) == '*') {
//                            answer.number = answer.number * ans.number;
//                            answer.expression.append(" * ").append(ans.expression);
//                        } else {
//                            answer.number = answer.number / ans.number;
//                            answer.expression.append(" / ").append(ans.expression);
//                        }
//                    } else {
//                        answer.number = ans.number;
//                        answer.expression.append(ans.expression);
//                    }
//                    previousPoz = i;
//                }
//
//            }
//            answer.expression.append(" )");
//
//            // возвращаем полученный результат
//            return answer;
//
//        } else {// если это простое число или любая кость
//
//            // разбиваем строку на коэффициенты кости
//            String[] expressions = rollS.split("[dD]");
//
//            if (expressions.length == 1) {// обычное число без d (не кость)
//                return new RollAnswer(Integer.parseInt(rollS), new StringBuilder().append(Integer.parseInt(rollS)));
//
//            } else if (expressions.length == 2) {// это кость
//
//                // количество граней на кости
//                int n = Integer.parseInt(expressions[1].trim());
//                // есть ли коэффициэнт перед костью
//                if (expressions[0].length() == 0) {// бросаем простую кость
//                    int roll = roll(n);
//                    RollAnswer answer = new RollAnswer(roll, new StringBuilder("<").append(roll).append(">"));
//                    // на двадцатых костях считаем особые числа
//                    if (n == 20 && roll == 1) answer.numberOfOnes++;
//                    if (n == 20 && roll == 20) answer.numberOfTwenties++;
//                    return answer;
//
//                } else {// бросаем кость несколько раз
//
//                    // количество костей
//                    int times = Integer.parseInt(expressions[0].trim());
//                    // кидаем кости и считаем результат
//                    RollAnswer answer = new RollAnswer();
//                    answer.expression.append("(");
//                    for (int i = 0; i < times; i++) {
//                        // кидаем одну из костей
//                        int roll = roll(n);
//                        // добавляем все в красивую строку
//                        answer.expression.append(" <").append(roll).append("> ");
//                        if (i != times - 1) answer.expression.append('+');
//                        // добавляем то что выпало
//                        answer.number += roll;
//                        // если кидали 20ку и выпали особые добавляем их к счетчику в ответе
//                        if (n == 20 && roll == 1) answer.numberOfOnes++;
//                        if (n == 20 && roll == 20) answer.numberOfTwenties++;
//                    }
//                    answer.expression.append(")");
//                    return answer;
//                }
//            } else // если там несколько r или вообще пусто
//                throw new java.lang.NumberFormatException();
//
//        }
//    }

    void addCounterAndReaction(MessageReceivedEvent event, int numberOfOnes, int numberOfTwenties) {
        // для d20
        if (numberOfOnes > 0 || numberOfTwenties > 0) {

            // реакция бота
            if (numberOfOnes > 0) {
                event.getMessage().addReaction("\uD83E\uDD2F").queue();
            }
            if (numberOfTwenties > 0) {
                event.getMessage().addReaction("\uD83C\uDF87").queue();
            }


            // находим игрока кинувшего кости
            PlayerData currentPlayer = guilds[currentGuildNumber].getPlayerByEvent(event);

            // создаем нового если он пуст
            if (currentPlayer == null) {
                // создаем нового игрока
                currentPlayer = new PlayerData();
                currentPlayer.playerId = event.getAuthor().getIdLong();
                guilds[currentGuildNumber].playersCurrentData.add(currentPlayer);
                Point save = getPlayerPointsFromFileById(event.getGuild().getIdLong(), event.getAuthor().getIdLong());// todo разобраться что делает этот метод и дать ему нормальное имя!
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
                event.getChannel().sendMessage(getUTF_8(" ======== Уфф, это твоя двадцатая единица!========")).queue();
            }
            if (currentPlayer.allNumberOfTwenties / 20 > twentyTwentiesCount) {
                event.getChannel().sendMessage(getUTF_8(" ======== Ура, это твоя двадцатая " + guilds[currentGuildNumber].masterDice + "! ======== ")).queue();
            }
        }

    }

    // =================================== вспомогательные методы ===================================

    String getUNICODE(String utf8String) {
        // строку utf-8 в байт массив, а затем байт массив в Unicode
        return new String(utf8String.getBytes(UTF_8));
    }

    static String getUTF_8(String unicodeString) {
        // строку Unicode в байт массив, а затем байт массив в utf-8
        return new String(unicodeString.getBytes(), UTF_8);
    }

    StringBuilder getTextInt(int n) {
        // создаем строку вывода
        StringBuilder answer = new StringBuilder();
        // выводим цифры
        if (n != 0) {
            // ставим отрицательный знак если он есть
            boolean isNegative = false;
            if (n < 0) {
                isNegative = true;
                n = -n;
            }
            // разбиваем число на цифры
            int[] numbers = new int[String.valueOf(n).length()];
            for (int i = 0; i < numbers.length; i++) {
                numbers[i] = n % 10;
                n /= 10;
            }
            // выводим пять строк
            for (int linesIterator = 0; linesIterator < 5; linesIterator++) {
                // выводим отрицательный знак, если он есть
                if (isNegative) {
                    answer.append(NUMBERS_CODES[10][linesIterator]).append("  ");
                }
                // выводим число наоборот
                for (int numbersIterator = numbers.length - 1; numbersIterator >= 0; numbersIterator--) {
                    answer.append(NUMBERS_CODES[numbers[numbersIterator]][linesIterator]).append("  ");
                }
                // завершаем строку
                answer.append("\n");
            }
        } else {
            // выводим пять строк нуля
            for (int linesIterator = 0; linesIterator < 5; linesIterator++) {
                answer.append(NUMBERS_CODES[0][linesIterator]).append("  ").append("\n");
            }
        }

        // возвращаем результат
        return answer;
    }

    String getAuthorName(MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {// личное сообщение
            return getUNICODE(event.getAuthor().getName());
        } else {// сообщение с сервера (обращаемся по нику а не по имени)
            return getUNICODE(Objects.requireNonNull(event.getMember()).getEffectiveName());
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

    Point getPlayerPointsFromFileById(long guildId, long memberId) {
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
            } catch (java.lang.NumberFormatException e) {
                System.out.println(TAG + ": guild=" + guildId + " no roll_channel = " +
                        rootObject.get("roll_channel").getAsString());
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
        } catch (java.lang.IllegalStateException e) {
            e.printStackTrace();
            // если структуры в файле нет, создаем новую со значениями по умолчанию
            System.out.println(TAG + ": create new json structure");
            rootObject = new JsonObject();
            rootObject.addProperty("version_code", "1");
            rootObject.addProperty("_id", Long.toString(guildId));
            rootObject.addProperty("roll_channel", "");
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


}

class Point {
    int numberOfOnes;
    int numberOfTwenties;

    public Point(int numberOfOnes, int numberOfTwenties) {
        this.numberOfOnes = numberOfOnes;
        this.numberOfTwenties = numberOfTwenties;
    }
}


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

// статистика одного игрока
class PlayerData {
    long playerId;
    int numberOfOnes;
    int numberOfTwenties;

    int allNumberOfOnes;
    int allNumberOfTwenties;
}

// результат кидания костей
class RollAnswer {
    // численный ответ
    int number;
    // текстовая запись выражения
    StringBuilder expression;
    // счетчики
    int numberOfOnes;
    int numberOfTwenties;
    // позиция ошибки
    int errorPoz;
    // длинна проверенной строки
    int checkedCorrectLength;

    public RollAnswer() {
        this.number = 0;
        this.expression = new StringBuilder();
        this.numberOfOnes = 0;
        this.numberOfTwenties = 0;
        this.errorPoz = -1;
    }

    public RollAnswer(int number, StringBuilder expression) {
        this.number = number;
        this.expression = expression;
        this.numberOfOnes = 0;
        this.numberOfTwenties = 0;
        this.errorPoz = -1;
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

