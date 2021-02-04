import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
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

    // рандомайзер
    Random r = new Random(System.currentTimeMillis());

    // врремя запуска бота
    static long startTimeMillis;

    // массив гильдий, в которых состоит бот
    static GuildData[] guilds;


    // метод запуска бота
    public static void main(String[] args) {//RollBot#0469 ▼►▲◄

        // builder аккаунта бота
        JDABuilder builder = JDABuilder.createDefault(TokenClass.BOT_TOKEN);

        // добавляем экземпляр этого класса в качестве обработчика сообщений
        builder.addEventListeners(new RollBot());

        startTimeMillis = System.currentTimeMillis();

        // логинимся в дискорде
        try {

            //аккаунт бота
            //JDA api =
            builder.build();
            System.out.println(TAG + ":Log in success!");

        } catch (LoginException e) {
            System.out.println(TAG + ":Log in error!");
            e.printStackTrace();
            System.exit(1);
        }


    }

    // отработает 1 раз когда бот проснулся и подключился
    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        super.onReady(event);
        System.out.println(TAG + ":Connected!");

        // выставляем статус бота онлайн
        event.getJDA().getPresence().setStatus(OnlineStatus.ONLINE);

        // получаем гильдии к которым подключен бот
        List<Guild> temp = event.getJDA().getGuilds();
        guilds = new GuildData[temp.size()];
        for (int i = 0; i < temp.size(); i++) {
            guilds[i] = new GuildData(temp.get(i).getIdLong());

            // загружаем данные игроков из сохранения
            getData(guilds[i]);
        }

    }

    // метод отрабатывающий при получении сообщения
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        // выводим сообщение в лог
        if (event.isFromType(ChannelType.PRIVATE)) {// личное сообщение
            System.out.printf(TAG + ":[Private] %s: %s\n",
                    getUNICODE(event.getAuthor().getName()),
                    getUNICODE(event.getMessage().getContentDisplay())
            );

        } else {// сообщение с сервера
            System.out.printf(
                    TAG + ":[ Server ][%s][%s] %s(%s): %s\n",
                    getUNICODE(event.getGuild().getName()), // гильдия (сервер)
                    getUNICODE(event.getTextChannel().getName()), // канал
                    getUNICODE(Objects.requireNonNull(event.getMember()).getEffectiveName()),// никнейм специфичный для данной гильдии написавшего
                    getUNICODE(event.getAuthor().getName()), // имя написавшего
                    getUNICODE(event.getMessage().getContentDisplay()) // сообщение
            );

            // отфильтровываем сообщения только в нужном канале
            if (
                    getUNICODE(event.getGuild().getName()).equals("РоллПати") &&
                            !getUNICODE(event.getTextChannel().getName()).equals("кидальня-костей")
            ) {
                return;
            }
        }

        // отфильтровываем сообщения от самого бота и пустые сообщения
        if (event.getAuthor().isBot() || event.getMessage().getContentDisplay().length() == 0) {
            return;
        }


        // =========================================== команда для бота (для всех чатов) ===========================================
        String msg = getUNICODE(event.getMessage().getContentDisplay());
        if (msg.charAt(0) == '/') {

            // ------------------------------------------ кинуть кость ------------------------------------------
            if (msg.charAt(1) == 'd' || msg.charAt(1) == 'D' // && msg.charAt(2) >= '0' && msg.charAt(2) <= '9'
            ) {
                // проверка на валидные значения
                try {
                    // кидаем кости и выводим сообщение
                    int n = Integer.parseInt(msg.substring(2));
                    int answer = roll(n);
                    event.getChannel().sendMessage(getUTF_8("Rolled by " + getAuthorName(event) + ": d" + n + "= " + answer + "\n" + getTextInt(answer))).queue();

                    // реакция бота и счетчик
                    if (n == 20) {
                        if (answer == 1)
                            addCounterAndReaction(event, 1, 0);
                        if (answer == 20)
                            addCounterAndReaction(event, 0, 1);
                    }

                } catch (java.lang.NumberFormatException e) {
                    event.getChannel().sendMessage(getUTF_8("Вы ввели некорректное число для броска! Читайте /help ")).queue();
                }
            }

            // ------------------------------------------ кинуть выражение ------------------------------------------
            if (msg.charAt(1) == 'r' || msg.charAt(1) == 'R' // && msg.charAt(2) >= '0' && msg.charAt(2) <= '9'
            ) {
                // проверка на валидные значения
                try {
                    RollAnswer answer = getRollAnswer(msg.substring(2));

                    event.getChannel().sendMessage(getUTF_8(
                            "Rolled by " + getAuthorName(event) + ":" + answer.expression + "  =  " + answer.number + "\n" + getTextInt(answer.number)
                    )).queue();

                    // реакция бота и счетчик
                    addCounterAndReaction(event, answer.numberOfOnes, answer.numberOfTwenties);

                } catch (java.lang.NumberFormatException e) {
                    event.getChannel().sendMessage(getUTF_8("Вы ввели некорректное выражение для броска! Читайте /help ")).queue();
                }
            }

            // ------------------------------------------ статистика ------------------------------------------
            if (msg.equals("/stat") && !event.isFromType(ChannelType.PRIVATE)) {
                // выводим статистику единиц и двадцаток
                ArrayList<PlayerData> playersData = getGuildFromListById(event.getGuild().getIdLong()).playersCurrentData;
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

            // ------------------------------------------ помощь ------------------------------------------
            if (msg.equals("/help")) {
                event.getChannel().sendMessage(getUTF_8("Привет, я " + TAG +
                        "\n Вот список доступных команд:" +
                        "\n\t\t/dN (d10, d3, d100..) - кинуть кость и вывести получившееся значение (от 1 до N)," +
                        "\n\t\t/r 2d2 * 2 + d2 - 5  - кинуть кости, посчитать выражение и вывести ответ " +
                        "\n\t\t\t(Поддерживаются + * / - dN KdN. Скобки пока не поддерживаются!)," +
                        "\n\t\t/stat - количество двадцаток и единиц за текущую игру," +
                        "\n\t\t/help - показ этого сообщения," +
                        "\n\t\t/exit - завершение работы бота"
                )).queue();
            }

            // ------------------------------------------ отладочный  ------------------------------------------
            if (msg.equals("/inf") && !event.isFromType(ChannelType.PRIVATE)) {
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

            // ------------------------------------------ команда выхода ------------------------------------------
            if (msg.equals("/exit")) {
                // выводим время работы бота
                long millis = (System.currentTimeMillis() - startTimeMillis) % 1000;
                long second = ((System.currentTimeMillis() - startTimeMillis) / 1000) % 60;
                long minute = ((System.currentTimeMillis() - startTimeMillis) / (1000 * 60)) % 60;
                long hour = ((System.currentTimeMillis() - startTimeMillis) / (1000 * 60 * 60)) % 24;

                // прощаемся
                event.getChannel().sendMessage(getUTF_8(
                        "Bye, bye " + getAuthorName(event)
                                + ".. Working time: "
                                + String.format("%02d:%02d:%02d.%d", hour, minute, second, millis)
                )).queue();

                // спим 2 секунды
                try{
                    Thread.sleep(2000);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }

                // задаем оффлайн статус (чтобы он отобразился сразу)
                event.getJDA().getPresence().setStatus(OnlineStatus.OFFLINE);

                // спим 2 секунды
                try{
                    Thread.sleep(2000);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                
                System.exit(0);
            }
        }

    }


    String getUNICODE(String utf8String) {
        // строку utf-8 в байт массив, а затем байт массив в Unicode
        return new String(utf8String.getBytes(UTF_8));
    }

    String getUTF_8(String unicodeString) {
        // строку Unicode в байт массив, а затем байт массив в utf-8
        return new String(unicodeString.getBytes(), UTF_8);
    }

    String getAuthorName(MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {// личное сообщение
            return getUNICODE(event.getAuthor().getName());
        } else {// сообщение с сервера (обращаемся по нику а не по имени)
            return getUNICODE(Objects.requireNonNull(event.getMember()).getEffectiveName());
        }
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

    int roll(int n) {
        if (n <= 0) throw new java.lang.NumberFormatException();
        //return (int) Math.round((Math.random() * (n - 1))) + 1;
        return (r.nextInt(n)) + 1;
    }

    RollAnswer getRollAnswer(String rollS) {
        rollS = rollS.trim();

        if (rollS.contains("+")) {// разбиваем выражение на слагаемые и передаем дальше
            // разбиваем строку на отдельные суммируемые части
            String[] expressions = rollS.split("\\+");

            // высчитываем каждую часть и суммируем все
            RollAnswer answer = new RollAnswer();
            answer.expression.append("( ");
            for (int i = 0; i < expressions.length - 1; i++) {
                // считаем конкретную часть
                RollAnswer ans = getRollAnswer(expressions[i]);
                // суммируем ее с остальными
                answer.expression.append(ans.expression).append(" + ");
                answer.number += ans.number;
                answer.numberOfOnes += ans.numberOfOnes;
                answer.numberOfTwenties += ans.numberOfTwenties;
            }
            RollAnswer ans = getRollAnswer(expressions[expressions.length - 1]);
            answer.expression.append(ans.expression).append(" )");
            answer.number += ans.number;

            // возвращаем полученный результат
            return answer;

        } else if (rollS.contains("-")) {// разбиваем выражение на вычитаемые и передаем дальше
            // разбиваем строку на отдельные вычитаемые части
            String[] expressions = rollS.split("[-]");

            // высчитываем каждую часть и суммируем все
            RollAnswer answer = new RollAnswer();
            answer.expression.append("( ");
            // если перед первым операндом не стоит отрицательный знак
            if (expressions[0].length() != 0) {
                RollAnswer ans = getRollAnswer(expressions[0]);
                answer.expression.append(ans.expression).append(" - ");
                answer.number += ans.number;
                answer.numberOfOnes += ans.numberOfOnes;
                answer.numberOfTwenties += ans.numberOfTwenties;
            } else {
                answer.expression.append('-');
            }

            // вычитаем обычные части
            for (int i = 1; i < expressions.length; i++) {
                RollAnswer ans = getRollAnswer(expressions[i]);
                answer.expression.append(ans.expression);
                if (i == expressions.length - 1) {
                    answer.expression.append(" )");
                } else {
                    answer.expression.append(" - ");
                }
                answer.number -= ans.number;
                answer.numberOfOnes += ans.numberOfOnes;
                answer.numberOfTwenties += ans.numberOfTwenties;
            }

            // возвращаем полученный результат
            return answer;

        } else if (rollS.contains("*") || rollS.contains("/")) {// разбиваем выражение на множители и передаем дальше


            // пробегаемся по строке
            RollAnswer answer = new RollAnswer();
            answer.expression.append("( ");
            answer.number = 1;
            int previousPoz = -1;
            for (int i = 1; i < rollS.length(); i++) {

                if (rollS.charAt(i) == '*' || rollS.charAt(i) == '/' || i == rollS.length() - 1) {
                    // считаем отдельную часть
                    RollAnswer ans;
                    if (i == rollS.length() - 1) {
                        ans = getRollAnswer(rollS.substring(previousPoz + 1));
                    } else {
                        ans = getRollAnswer(rollS.substring(previousPoz + 1, i));
                    }

                    // записываем посчитанное в общую переменную
                    answer.numberOfOnes += ans.numberOfOnes;
                    answer.numberOfTwenties += ans.numberOfTwenties;
                    if (previousPoz != -1) {
                        if (rollS.charAt(previousPoz) == '*') {
                            answer.number = answer.number * ans.number;
                            answer.expression.append(" * ").append(ans.expression);
                        } else {
                            answer.number = answer.number / ans.number;
                            answer.expression.append(" / ").append(ans.expression);
                        }
                    } else {
                        answer.number = ans.number;
                        answer.expression.append(ans.expression);
                    }
                    previousPoz = i;
                }

            }
            answer.expression.append(" )");

            // возвращаем полученный результат
            return answer;

        } else {// если это простое число или кость

            // разбиваем строку на коэффициенты кости
            String[] expressions = rollS.split("[dD]");

            if (expressions.length > 2 || expressions.length == 0) {// если там несколько r
                throw new java.lang.NumberFormatException();

            } else if (expressions.length == 2) {// бросаем кость несколько раз

                if (expressions[0].length() == 0) {// бросаем простую кость
                    int n = Integer.parseInt(expressions[1]);
                    int roll = roll(n);
                    RollAnswer answer = new RollAnswer(roll, new StringBuilder("<").append(roll).append(">"));
                    // на двадцатых костях считаем особые числа
                    if (n == 20 && roll == 1) answer.numberOfOnes++;
                    if (n == 20 && roll == 20) answer.numberOfTwenties++;
                    return answer;
                }

                // количество граней на кости
                int n = Integer.parseInt(expressions[1].trim());
                // количество костей
                int times = Integer.parseInt(expressions[0].trim());

                // кидаем кости и считаем результат
                RollAnswer answer = new RollAnswer();
                answer.expression.append("(");
                for (int i = 0; i < times; i++) {
                    int roll = roll(n);
                    // на двадцатых костях считаем особые числа
                    if (n == 20 && roll == 1) answer.numberOfOnes++;
                    if (n == 20 && roll == 20) answer.numberOfTwenties++;
                    answer.expression.append(" <").append(roll);
                    if (i == times - 1) {
                        answer.expression.append("> )");
                    } else {
                        answer.expression.append("> +");
                    }
                    answer.number += roll;
                }

                return answer;

            } else {// обычное число
                return new RollAnswer(Integer.parseInt(rollS), new StringBuilder().append(Integer.parseInt(rollS)));
            }

        }
    }

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

            // определяем текущую гильдию
            GuildData currentGuild = getGuildFromListById(event.getGuild().getIdLong());

            // находим игрока кинувшего кости
            PlayerData currentPlayer = currentGuild.getPlayerByEvent(event);

            // создаем нового если он пуст
            if (currentPlayer == null) {
                // создаем нового игрока
                currentPlayer = new PlayerData();
                currentPlayer.playerId = event.getAuthor().getIdLong();
                currentGuild.playersCurrentData.add(currentPlayer);
                Point save = getData(event.getGuild().getIdLong(), event.getAuthor().getIdLong());
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
            saveData(
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
                event.getChannel().sendMessage(getUTF_8(" ======== Ура, это твоя двадцатая двадцатка! ======== ")).queue();
            }
        }

    }


    Point getData(long guildId, long memberId) {

// достать данные
//        JsonElement jsonElement = JsonParser.parseString("{\"message\":\"Hi\",\"place\":{\"name\":\"World!\"}}");
//
//        JsonObject rootObject = jsonElement.getAsJsonObject(); // чтение главного объекта
//        String message = rootObject.get("message").getAsString(); // получить поле "message" как строку
//        JsonObject childObject = rootObject.getAsJsonObject("place"); // получить объект Place
//        String place = childObject.get("name").getAsString(); // получить поле "name"
//        System.out.println(message + " " + place); // напечатает "Hi World!"*/


//
//
//        // парсим данные в обьект
//        JsonObject rootObject = new JsonObject(); // создаем главный объект
//        rootObject.addProperty("message", "Hi"); // записываем текст в поле "message"
//        JsonObject childObject = new JsonObject(); // создаем объект Place
//        childObject.addProperty("name", "World!"); // записываем текст в поле "name" у объект Place
//        rootObject.add("place", childObject); // сохраняем дочерний объект в поле "place"
//
//        Gson gson = new Gson();
//        String json = gson.toJson(rootObject); // генерация json строки
//        System.out.println(json); // напечатает "{"message":"Hi","place":{"name":"World!"}}"

/*
{
  "version_code":"1",
  "guilds":
  [
    {
      "_id":"getGuild().getId()",
      "members":[
        {
          "_id":"getAuthor().getId()"
          "pointers_count":"какое-нибудь количество очков"
        }
      ]
    }
  ]
}




{
  "version_code":"1",
  "_id":"getGuild().getId()",
  "members":[
    {
      "_id":"getAuthor().getId()"
      "pointers_count":"какое-нибудь количество очков"
    }
  ]
}

{
  "version_code":"1",
  "_id":"689098857849552926",
  "members":[
    {
      "_id":517628304967204876,
      "number_of_ones":1,
      "number_of_twenties":1
    }
  ]
}

*/
        try {

            // объект корневого каталога
            File rootDir = new File("__saved_data");
            if (rootDir.mkdir()) {
                System.out.println(TAG + ": Directory created");
            }


            // файл с данными конкретной гильдии
            File guild_data = new File(rootDir, guildId + ".txt");

            if (guild_data.createNewFile()) {
                System.out.println(TAG + ": New file " + guildId + ".txt created!");
            }


            // читаем данные из файла
            FileReader reader = new FileReader(guild_data);
            StringBuilder contains = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                contains.append((char) c);
            }
            reader.close();


            // преобразуем данные из файла в json
            // чтение главного объекта
            JsonObject rootObject;
            JsonArray membersObject;
            try {
                rootObject = JsonParser.parseString(contains.toString()).getAsJsonObject();
                membersObject = rootObject.getAsJsonArray("members");


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

            } catch (java.lang.IllegalStateException e) {
                e.printStackTrace();
                // игрока в сохраненных нет, возвращаем нули
                return new Point(0, 0);
            }

        } catch (IOException e) {
            e.printStackTrace();
            // игрока в сохраненных нет, возвращаем нули
            return new Point(0, 0);
        }

    }

    void getData(GuildData guild) {

        try {

            // объект корневого каталога
            File rootDir = new File("__saved_data");
            if (rootDir.mkdir()) {
                System.out.println(TAG + ": Directory created");
            }


            // файл с данными конкретной гильдии
            File guild_data = new File(rootDir, guild.guildId + ".txt");

            if (guild_data.createNewFile()) {
                System.out.println(TAG + ": New file " + guild.guildId + ".txt created!");
            }


            // читаем данные из файла
            FileReader reader = new FileReader(guild_data);
            StringBuilder contains = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                contains.append((char) c);
            }
            reader.close();


            // преобразуем данные из файла в json
            // чтение главного объекта
            JsonObject rootObject;
            JsonArray membersObject;
            try {
                rootObject = JsonParser.parseString(contains.toString()).getAsJsonObject();
                membersObject = rootObject.getAsJsonArray("members");

                // пробегаемся по всем участникам
                for (int i = 0; i < membersObject.size(); i++) {

                    // получаем одного участника
                    PlayerData player = new PlayerData();
                    player.playerId = membersObject.get(i).getAsJsonObject().get("_id").getAsLong();
                    player.allNumberOfOnes = membersObject.get(i).getAsJsonObject().get("number_of_ones").getAsInt();
                    player.allNumberOfTwenties = membersObject.get(i).getAsJsonObject().get("number_of_twenties").getAsInt();
                    guild.playersCurrentData.add(player);
                }

            } catch (java.lang.IllegalStateException e) {
                //e.printStackTrace();
            }

        } catch (IOException e) {
            //e.printStackTrace();
        }

    }

    void saveData(long guildId, long memberId, int appendNumberOfOnes, int appendNumberOfTwenties) {
        System.out.println(TAG + ":saveData g:" + guildId + " p:" + memberId);

        try {

            // объект корневого каталога
            File rootDir = new File("__saved_data");
            if (rootDir.mkdir()) {
                System.out.println(TAG + ": Directory created");
            }


            // файл с данными конкретной гильдии
            File guild_data = new File(rootDir, guildId + ".txt");

            if (guild_data.createNewFile()) {
                System.out.println(TAG + ": New file " + guildId + ".txt created!");
            }


            // читаем данные из файла
            FileReader reader = new FileReader(guild_data);
            StringBuilder contains = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                contains.append((char) c);
            }
            reader.close();


            // преобразуем данные из файла в json
            // чтение главного объекта
            JsonObject rootObject;
            JsonArray membersObject;
            try {
                rootObject = JsonParser.parseString(contains.toString()).getAsJsonObject();
                membersObject = rootObject.getAsJsonArray("members");

            } catch (java.lang.IllegalStateException e) {
                e.printStackTrace();
                // создание новой структуры
                System.out.println(TAG + ": create new json structure");
                rootObject = new JsonObject();
                rootObject.addProperty("version_code", "1");
                rootObject.addProperty("_id", "" + guildId);

                membersObject = new JsonArray();
                rootObject.add("members", membersObject);

            }


            // пополняем json новыми данными
            boolean writeFlag = false;
            for (int i = 0; i < membersObject.size(); i++) {
                // получаем одного участника и проверяем не его ли это id
                if (membersObject.get(i).getAsJsonObject().get("_id").getAsLong() == memberId) {
                    // меняем значения на новые
                    membersObject.get(i).getAsJsonObject().addProperty(
                            "number_of_ones",
                            membersObject.get(i).getAsJsonObject().get("number_of_ones").getAsInt() + appendNumberOfOnes
                    );

                    membersObject.get(i).getAsJsonObject().addProperty(
                            "number_of_twenties",
                            membersObject.get(i).getAsJsonObject().get("number_of_twenties").getAsInt() + appendNumberOfTwenties
                    );
                    writeFlag = true;
                    break;
                }
            }
            // создаем нового участника
            if (!writeFlag) {
                JsonObject newMember = new JsonObject();
                newMember.addProperty("_id", memberId);
                newMember.addProperty("number_of_ones", appendNumberOfOnes);
                newMember.addProperty("number_of_twenties", appendNumberOfTwenties);

                membersObject.add(newMember);

            }


            // пишем json в файл
            String data = new Gson().toJson(rootObject);
            FileWriter writer = new FileWriter(guild_data);
            writer.append(data);
            writer.flush();

            // закрываем файл
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    GuildData getGuildFromListById(long guildId) {
        // определяем текущую гильдию
        for (GuildData guild : guilds) {
            if (guild.guildId == guildId) {
                return guild;
            }
        }
        return null;
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

    public RollAnswer() {
        this.number = 0;
        this.expression = new StringBuilder();
        this.numberOfOnes = 0;
        this.numberOfTwenties = 0;
    }

    public RollAnswer(int number, StringBuilder expression) {
        this.number = number;
        this.expression = expression;
        this.numberOfOnes = 0;
        this.numberOfTwenties = 0;
    }
}


// helpful code:
// event.getAuthor().getName() + event.getAuthor().getDiscriminator()
//event.getMessage().addReaction("\uD83C\uDFB2").queue();//":game_die:"
