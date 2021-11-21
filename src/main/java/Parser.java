import java.util.Random;

public class Parser {

    // отладка
    //private static String append;


    public static RollAnswer parseRollString(
            String rollString, int mainDice, Random randomizer, int lowerThreshold, int upperThreshold
    ) {
        //append = "";
        // создаем ответ
        RollAnswer rollAnswer = new RollAnswer(
                new StringBuilder(
                        rollString.replaceAll(" ", "")// удаляем пробелы
                ),
                mainDice,
                randomizer,
                lowerThreshold,
                upperThreshold
        );
        try {
            // заполняем его
            rollAnswer.number = sumCheck(rollAnswer);
        } catch (ArithmeticException e) {
            if (e.getMessage().equals("/ by zero")) {
                rollAnswer.errorPoz = -2;
            } else {
                rollAnswer.errorPoz = 0;
                e.printStackTrace();
            }
        } catch (NumberFormatException e) {
            rollAnswer.errorPoz = -3;
        }


        // парсинг мог просто недойти до конца - значит в веденной строке ошибка
        if (rollAnswer.tempRollString.length() != 0) {
            rollAnswer.errorPoz = rollAnswer.checkedCorrectLength;
        }

        // возвращаем
        return rollAnswer;
    }


    // =========================================== методы парсинга выражений ===========================================

    // ищем слагаемые в строке
    private static int sumCheck(RollAnswer forResult) {
        //System.out.println(append + "sumCheck: '" + forResult.tempRollString + '\'');
        int returnAnswer = 0;

        // нулевая строка сразу нет
        if (forResult.tempRollString.length() == 0) {
            forResult.errorPoz = forResult.checkedCorrectLength;
            //System.out.println("empty error 1");
            return returnAnswer;
        }

        // сразу отдаем на проверку
        returnAnswer = multiplyCheck(forResult);
        //System.out.println(append + "1_" + forResult.tempRollString);

        // если возможно идем дальше, проверяя на знаки + -
        while (forResult.tempRollString.length() != 0 && forResult.errorPoz == -1) {
            if ((forResult.tempRollString.charAt(0) != '+' && forResult.tempRollString.charAt(0) != '-')) {
                break;
            }
            boolean isPlus = forResult.tempRollString.charAt(0) == '+';
            // нашли знак, отмечаем, стираем
            forResult.expression.append((isPlus) ? (" + ") : (" - "));
            forResult.tempRollString.deleteCharAt(0);
            forResult.checkedCorrectLength++;

            // раз есть знак, идем дальше
            int result = multiplyCheck(forResult);
            if (isPlus) {
                returnAnswer += result;
            } else
                returnAnswer -= result;

            //System.out.println(append + "2_" + forResult.tempRollString);
        }
        //System.out.println(append + "sumCheck: returnAnswer=" + returnAnswer);
        return returnAnswer;
    }

    // ищем множители в строке
    private static int multiplyCheck(RollAnswer forResult) {
        //System.out.println(append + "multiplyCheck: '" + forResult.tempRollString + '\'');
        int returnAnswer = 0;

        // нулевая строка сразу нет
        if (forResult.tempRollString.length() == 0) {
            forResult.errorPoz = forResult.checkedCorrectLength;
            //System.out.println("empty error 2");
            return returnAnswer;
        }

        // сразу отдаем на проверку
        returnAnswer = bracketsCheck(forResult);
        //System.out.println(append + "1_" + forResult.tempRollString);

        // если возможно идем дальше, проверяя на знаки * /
        while (forResult.tempRollString.length() != 0 && forResult.errorPoz == -1) {
            if ((forResult.tempRollString.charAt(0) != '*' && forResult.tempRollString.charAt(0) != '/')) {
                break;
            }
            boolean isMultiply = forResult.tempRollString.charAt(0) == '*';
            // нашли знак, отмечаем, стираем
            forResult.expression.append((isMultiply) ? (" * ") : (" / "));
            forResult.tempRollString.deleteCharAt(0);
            forResult.checkedCorrectLength++;

            // раз есть знак, идем дальше
            int result = bracketsCheck(forResult);
            if (isMultiply) {
                returnAnswer *= result;
            } else
                returnAnswer /= result;

            //System.out.println(append + "2_" + forResult.tempRollString);
        }
        //System.out.println(append + "multiplyCheck: returnAnswer=" + returnAnswer);
        return returnAnswer;
    }

    // ищем выражение в скобках или кость/число в строке
    private static int bracketsCheck(RollAnswer forResult) {
        //System.out.println(append + "bracketsCheck: '" + forResult.tempRollString + '\'');
        int returnAnswer = 0;

        // нулевая строка сразу нет
        if (forResult.tempRollString.length() == 0) {
            forResult.errorPoz = forResult.checkedCorrectLength;
            //System.out.println("empty error 3");
            return returnAnswer;
        }

        // конструкция в скобках
        if (forResult.tempRollString.charAt(0) == '(') {
            // нашли скобку, удаляем ее
            forResult.tempRollString.deleteCharAt(0);
            forResult.checkedCorrectLength++;
            forResult.expression.append('(');
            //append = append + ' ';

            // считаем выражение внутри скобок и суммируем это с основным выражением
            returnAnswer = sumCheck(forResult);
            //System.out.println(append + "1_" + forResult.tempRollString);

            // не пустая ли строка
            if (forResult.tempRollString.length() == 0) {
                forResult.errorPoz = forResult.checkedCorrectLength;
                //System.out.println("empty error 4");
                return returnAnswer;
            }

            // если ошибок нет
            if (forResult.errorPoz == -1) {

                // предыдущий метод должен был дойти до закрывающей скобки
                if (forResult.tempRollString.charAt(0) == ')') {
                    // нашли скобку, удаляем ее
                    forResult.tempRollString.deleteCharAt(0);
                    forResult.checkedCorrectLength++;
                    forResult.expression.append(")");

                    //append = append.substring(0, append.length() - 1);
                } else {
                    // если закрывающей скобки нет, это ошибка
                    forResult.errorPoz = forResult.checkedCorrectLength;
                    //System.out.println(append + "error with ()");
                }

            }
        } else {// просто множитель
            returnAnswer = rollCheck(forResult);
            //System.out.println(append + "2_" + forResult.tempRollString);
        }
        //System.out.println(append + "bracketsCheck: returnAnswer=" + returnAnswer);
        return returnAnswer;
    }

    // считываем кость/число из строки
    private static int rollCheck(RollAnswer forResult) {
        //System.out.println(append + "rollCheck: '" + forResult.tempRollString + '\'');// 55 -55 55d55
        int returnAnswer = 0;

        // нулевая строка сразу нет
        if (forResult.tempRollString.length() == 0) {
            forResult.errorPoz = forResult.checkedCorrectLength;
            //System.out.println("empty error 5");
            return returnAnswer;
        }

        //System.out.println("forResult.tempRollString 1= '" + forResult.tempRollString + '\'');

        // считываем минус в начале числа, если он есть
        boolean minus = forResult.tempRollString.charAt(0) == '-';
        if (minus) forResult.tempRollString.deleteCharAt(0);

        //System.out.println("forResult.tempRollString 2= '" + forResult.tempRollString + '\'');

        int firstNumber = 0;
        // считываем первую часть кости или число
        while (forResult.tempRollString.length() != 0) {// пока строка не пустая
            // и пока встречаем цифры
            if (!(forResult.tempRollString.charAt(0) >= '0' &&
                    forResult.tempRollString.charAt(0) <= '9')) break;

            // символ в цифру todo проверка: не слишком ли большое число (идеально будет сделать прямо здесь, где происходит его расчет)
            firstNumber = firstNumber * 10 + (forResult.tempRollString.charAt(0) - '0');// [code 5] - [code 0] = 5


            //System.out.println("forResult.tempRollString 4= '" + forResult.tempRollString + '\'');
            forResult.tempRollString.deleteCharAt(0);
            forResult.checkedCorrectLength++;

            //System.out.println("forResult.tempRollString 5= '" + forResult.tempRollString + '\'');
        }

        // проверяем, есть ли дальше кость
        boolean isDiceMode = forResult.tempRollString.length() != 0;// если не конец строки
        if (isDiceMode) isDiceMode = forResult.tempRollString.charAt(0) == 'd'; // считываем часть д
        // (если дальше какие-то другие символы, то мы их проверять не будем)

        //System.out.println("isDiceMode = " + isDiceMode);
        //System.out.println("forResult.tempRollString 3= '" + forResult.tempRollString + '\'');

        if (isDiceMode) {// пытаемся считать значение кости

            //System.out.println("forResult.tempRollString = " + forResult.tempRollString);
            forResult.tempRollString.deleteCharAt(0);
            forResult.checkedCorrectLength++;

            //System.out.println("forResult.tempRollString 2 = " + forResult.tempRollString);

            int diceValue = 0;

            // считываем номинал кости
            while (forResult.tempRollString.length() != 0) {// пока строка не пустая
                // и пока встречаем цифры
                if (!(forResult.tempRollString.charAt(0) >= '0' &&
                        forResult.tempRollString.charAt(0) <= '9')) break;

                // символ в цифру todo проверка: не слишком ли большое число (идеально будет сделать прямо здесь, где происходит его расчет)
                diceValue = diceValue * 10 + (forResult.tempRollString.charAt(0) - '0');// [code 5] - [code 0] = 5

                forResult.tempRollString.deleteCharAt(0);
                forResult.checkedCorrectLength++;
            }

            //System.out.println("forResult.tempRollString 11 = " + forResult.tempRollString + " diceValue=" + diceValue);

            // число после d пустое, ошибка
            if (diceValue == 0) {
                forResult.errorPoz = forResult.checkedCorrectLength;
                //System.out.println("2err");
            } else {
                // если есть число кости, то кидаем ее
                returnAnswer += rollDice(forResult, firstNumber, diceValue, minus);
            }
        } else {// простое число
            if (minus) {
                returnAnswer += -firstNumber;
                forResult.expression.append('-').append(firstNumber);
            } else {
                returnAnswer += firstNumber;
                forResult.expression.append(firstNumber);
            }
        }

        //System.out.println("d => " + forResult.tempRollString + " returnAnswer=" + returnAnswer + " m =" + minus + " count = " + firstNumber);
        return returnAnswer;
    }

    // распределяем броски кости
    private static int rollDice(RollAnswer forResult, int count, int value, boolean isMinus) {
        int returnAnswer = 0;

        // есть ли коэффициэнт перед костью
        if (count == 0 || count == 1) {// бросаем простую кость

            int roll = roll(value, forResult.mainDice, forResult.randomizer, forResult.lowerThreshold, forResult.upperThreshold);

            if (isMinus) {
                returnAnswer += -roll;
                forResult.expression.append("-<").append(roll).append('>');
            } else {
                returnAnswer += roll;
                forResult.expression.append('<').append(roll).append('>');
            }
            // на двадцатых костях считаем особые числа
            if (value == forResult.mainDice && roll == 1) forResult.numberOfOnes++;
            if (value == forResult.mainDice && roll == forResult.mainDice) forResult.numberOfTwenties++;

        } else {// бросаем кость несколько раз
            forResult.expression.append((isMinus) ? ("-(") : ('('));

            for (int i = 0; i < count; i++) {
                // кидаем одну из костей
                int roll = roll(value, forResult.mainDice, forResult.randomizer, forResult.lowerThreshold, forResult.upperThreshold);
                // добавляем все в красивую строку
                forResult.expression.append('<').append(roll).append('>');
                if (i != count - 1) forResult.expression.append('+');
                // добавляем то что выпало
                if (isMinus) {
                    returnAnswer -= roll;
                } else {
                    returnAnswer += roll;
                }
                // если кидали 20ку и выпали особые добавляем их к счетчику в ответе
                if (value == forResult.mainDice && roll == 1) forResult.numberOfOnes++;
                if (value == forResult.mainDice && roll == forResult.mainDice) forResult.numberOfTwenties++;
            }

            forResult.expression.append(')');
        }
        return returnAnswer;
    }

    // наконец бросаем одну отдельную кость!
    private static int roll(int diceValue, int mainDice, Random randomizer, int lowerThreshold, int upperThreshold) {

        // кость читов?
        if (diceValue == mainDice) {

            if (lowerThreshold <= upperThreshold) {
                // диапазон между lowerThreshold и upperThreshold
                if (upperThreshold <= diceValue) {
                    if (lowerThreshold >= 1) {
                        return randomizer.nextInt(upperThreshold - lowerThreshold + 1) + lowerThreshold;
                    } else {
                        return randomizer.nextInt(upperThreshold) + 1;
                    }

                } else {
                    if (lowerThreshold >= 1) {
                        return randomizer.nextInt(diceValue - lowerThreshold + 1) + lowerThreshold;
                    } else {
                        return randomizer.nextInt(diceValue) + 1;
                    }
                }
            } else {
                // если пересечения условий нет, то учитываем только upperThreshold
                if (upperThreshold <= diceValue) {
                    return randomizer.nextInt(upperThreshold) + 1;
                } else {
                    return randomizer.nextInt(diceValue) + 1;
                }
            }
        } else
            return randomizer.nextInt(diceValue) + 1;
    }


    /*
     * todo ошибки
     *
     *  /r--1
     *  Texnar13:-0-1  =  -1
     *
     * */


    public static class RollAnswer {// результат кидания костей
        // численный ответ
        int number = 0;
        // текстовая запись выражения которая сформируется в итоге
        StringBuilder expression = new StringBuilder();
        // счетчики
        int numberOfOnes = 0;
        int numberOfTwenties = 0;


        // позиция ошибки
        int errorPoz = -1;
        // длинна проверенной строки
        int checkedCorrectLength = 0;
        // временная строка без пробелов хранящая непрочтенные символы
        StringBuilder tempRollString;


        // ссылка на randomizer
        Random randomizer;
        // текущая главная кость
        int mainDice;
        // ограничения читов
        int lowerThreshold;
        int upperThreshold;


        private RollAnswer(StringBuilder expression, int mainDice, Random randomizer, int lowerThreshold, int upperThreshold) {
            this.mainDice = mainDice;
            this.tempRollString = new StringBuilder(expression.toString());
            this.randomizer = randomizer;
            this.lowerThreshold = lowerThreshold;
            this.upperThreshold = upperThreshold;
        }
    }

}
