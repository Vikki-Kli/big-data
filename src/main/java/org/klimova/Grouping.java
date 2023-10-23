package org.klimova;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Grouping {
    public static void main(String[] args) {

        byte[] all = null;
        try {
            all = Files.readAllBytes(Paths.get(args[0]));
        } catch (Exception e){
            System.out.println("Проверьте файл с исходными данными");
        }

        if (all == null) {
            return;
        }

        List<String> lines = Arrays.asList(new String(all).split("\n"));
        List<Set<String>> finalGroups = groupLines(lines);
        long countOfBigGroups = finalGroups.stream().filter(s -> s.size() > 1).count();

        try (FileWriter writer = new FileWriter("out.txt")) {
            int count = 1;
            writer.write("Количество групп с более чем 1 элементом: " + countOfBigGroups + "\n");
            for (Set<String> group : finalGroups) {
                writer.write("Группа " + count++ + "\n");
                for (String line : group) {
                    writer.write(line + "\n");
                }
            }
            System.out.println("Файл с результатами out.txt ищите в папке с исполняемым файлом");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static List<Set<String>> groupLines(List<String> lines) {

        class GraphNode {
            public final String value; //Значение в "" скобках, из которых состоит строка
            public final int position; //Порядковый номер значения в строке

            public GraphNode(String value, int position) {
                this.value = value;
                this.position = position;
            }
        }

        //Все элементы сгруппированы по их порядковому номеру в строке. Сравниваем текущий элемент со всеми элементами
        //с таким же порядковым номером, и если находим совпадение, смотрим, какой группе принадлежит найденное совпадение.

        //Set<String> - группа строк, сформированная согласно ТЗ
        List<Set<String>> groups = new ArrayList<>();
        //Map<String, Integer> - ассортимент того, что может находиться под одним порядковым номером.
        //String - элемент, Integer - номер группы, индекс в List - порядковый номер (позиция)
        List<Map<String, Integer>> fillingOfPositions = new ArrayList<>();
        //Записи о слиянии групп, когда у них внезапно появился общий элемент - <номер поглощенной группы, номер группы-поглотителя>
        Map<Integer, Integer> mergedGroups = new HashMap<>();

        for (String line : lines) {
            // Если строка битая или состоит из пустых элементов, она игнорируется
            if (line.matches("[;\"]*") || line.matches(".*[^;]\"[^;].*")) continue;
            String[] elementsOfLine = line.split(";");

            TreeSet<Integer> groupMatchings = new TreeSet<>();
            List<GraphNode> singleElements = new ArrayList<>();

            // Проходим по всем элементам текущей строки, ищем совпадения
            for (int i = 0; i < elementsOfLine.length; i++) {
                String element = elementsOfLine[i];

                // Нам нужен список элементов с той же позицией. Если текущая строка оказалась длиннее предыдущих,
                // списков позиций под ее последние элементы еще не существует, добавляем новый.
                if (fillingOfPositions.size() == i) fillingOfPositions.add(new HashMap<>());
                if (element.equals("\"\"") || element.equals("")) continue; //Пустые элементы нам не интересны
                Map<String, Integer> assortmentOfPosition = fillingOfPositions.get(i);

                // Проверяем, встречался ли нам уже такой элемент на этой позиции. Если да, то нам вернется номер группы,
                // которой он принадлежит, добавляем группу в список совпадений. Если совпадений не нашлось,
                // добавим элемент в список одиноких, записав его значение и позицию.
                Integer groupNumber = assortmentOfPosition.get(element);
                if (groupNumber != null) {
                    while (mergedGroups.containsKey(groupNumber)) { // Проверяем, не поглощена ли эта группа другой
                        groupNumber = mergedGroups.get(groupNumber);
                    }
                    groupMatchings.add(groupNumber);
                } else {
                    singleElements.add(new GraphNode(element, i));
                }
            }

            // Присваиваем строке номер группы. Если совпадение найдено, берем любую группу из списка, если нет - создаем новую
            int groupNumber;
            if (groupMatchings.isEmpty()) {
                groupNumber = groups.size();
                groups.add(new HashSet<>());
            } else {
                groupNumber = groupMatchings.first();
            }

            // Если совпадения найдены в нескольких группах, их нужно объединить
            for (int number : groupMatchings) {
                if (number != groupNumber) {
                    groups.get(groupNumber).addAll(groups.get(number));
                    groups.set(number, null);
                    mergedGroups.put(number, groupNumber);
                }
            }

            // Раскидываем элементы строки по спискам-позициям, чтобы они тоже участвовали в поиске
            for (GraphNode node : singleElements) {
                fillingOfPositions.get(node.position).put(node.value, groupNumber);
            }

            // Записываем строку в ее группу
            groups.get(groupNumber).add(line);
        }

        groups.removeIf(Objects::isNull);
        groups.sort(Comparator.comparingInt(Set::size));
        Collections.reverse(groups);
        return groups;
    }
}


