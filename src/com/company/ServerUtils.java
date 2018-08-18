//package com.company;
//
//import java.io.File;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.text.DateFormat;
//import java.text.DecimalFormat;
//import java.text.DecimalFormatSymbols;
//import java.text.ParseException;
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
///**
// * Утилитные методы общие для некторых серверных классов.
// * <p>
// * Created by dchernyshov on 25.10.17.
// */
//public class ServerUtils {
//    private static final Logger LOGGER = Logger.getLogger(ServerUtils.class.getSimpleName());
//
//    /**
//     * Получение списка History за промежуток времени и с определенным статусом
//     *
//     * @param begin        дата начала
//     * @param end          дата конца промежутка
//     * @param statuses     статусы
//     * @param scenarioType сценарий
//     * @return список записей истории
//     */
//    @SuppressWarnings("unchecked")
//    public static List<History> loadHistoryList(DateFormat dateFormat,
//                                                String begin,
//                                                String end,
//                                                Set<StatusEnum> statuses,
//                                                String scenarioType,
//                                                List<String> contractIds,
//                                                Provider<EntityManager> provider) throws ParseException {
//        Date beginDate = dateFormat.parse(begin);
//        Date endDate = dateFormat.parse(end);
//        endDate = new Date(endDate.getTime() + 1000 * 60 * 60 * 23 + 1000 * 60 * 59); // фикс времени 23:59
//
//        List<Long> ids = new ArrayList<>(contractIds.size());
//        for (String str : contractIds) {
//            ids.add(Long.valueOf(str));
//        }
//
//        if ("kd".equals(scenarioType)) scenarioType = Constants.SCENARY_KD;
//        if ("pd".equals(scenarioType)) scenarioType = Constants.SCENARY_PD;
//
//        List<StatusEnum> statusList = new ArrayList<>(statuses);
//
//        Query query = provider.get().createQuery("select h from History h WHERE (h.currentDate BETWEEN ?1 AND ?2)" +
//                "                                                    AND h.statusFk IN ?3" +
//                "                                                    AND (h.documentFk.scenarioType = ?4)" +
//                "                                                    AND h.documentFk.contractFk.id IN ?5"
//        );
//        query.setParameter(1, beginDate);
//        query.setParameter(2, endDate);
//        query.setParameter(3, statusList);
//        query.setParameter(4, scenarioType);
//        query.setParameter(5, ids);
//        List<History> result = new ArrayList<>(); // так результат будет хоть какой-то список, а не null
//        result.addAll(query.getResultList());
//        return result;
//    }
//
//    public static void fixErrorListIfBrokenForHistories(List<History> histories, Provider<EntityManager> provider) {
//        Set<Document> documents = new HashSet<>();
//        for (History history : histories) {
//            documents.add(history.getDocumentFk());
//        }
//        EntityManager entityManager = provider.get();
//        for (Document document : documents) {
//            fixErrorListIfBroken(document.getId(), entityManager);
//        }
//    }
//
//    public static void refreshHisotryList(List<History> histories, Provider<EntityManager> provider) {
//        EntityManager entityManager = provider.get();
//        for (History history : histories) {
//            entityManager.refresh(history);
//            for (ErrorListRow errorListRow : history.getErrorListRows()) {
//                entityManager.refresh(errorListRow);
//            }
//        }
//    }
//
//    /**
//     * Исправление ошибки не переноса ЛН.
//     *
//     * @param documentId документ, для которого правятся ЛН
//     */
//    public static Boolean fixErrorListIfBroken(Long documentId, EntityManager em) {
//        boolean isErrorListBroken = false;
//        Query docQuery = em.createQuery("from Document d where d.id = " + documentId.toString());
//        Document document = (Document) docQuery.getSingleResult();
//        List<History> historyList = document.getHistoryList();
//        DataUtils.sortHistoryListByDateAsc(historyList);
//        for (int i = 0; i < historyList.size() - 1; i++) {
//            History history = historyList.get(i);
//            if (history.getStatusFk().isReturnErrorlist()) {
//                List<ErrorListRow> ll = history.getErrorListRows();
//                // если пуст - ахтунг - нашелся пустой возврат!
//                if (ll.isEmpty() || isErrorListDiffer(historyList.get(i + 1), history)) {
//                    if (!ll.isEmpty()) {
//                        for (ErrorListRow errorListRow : ll) {
//                            deleteErrorListRow(errorListRow.getId(), em);
//                        }
//                    }
//                    copyErrorList(historyList.get(i + 1).getId(), history.getId(), em);
//                    isErrorListBroken = true;
//                    i++;
//                }
//            }
//        }
//        return isErrorListBroken;
//    }
//
//
//    private static boolean isErrorListDiffer(History historyWithRightErrors, History history) {
//        if (!historyWithRightErrors.getErrorListRows().isEmpty()) {
//            if (!history.getErrorListRows().isEmpty()) {
//                if (!historyWithRightErrors.getErrorListRows().containsAll(history.getErrorListRows())) {
//                    return true;
//                }
//            } else {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    /**
//     * Удаление строки-замечания ЛН вместе с прикрепленным файлом
//     *
//     * @param rowId ид строки
//     */
//    public static void deleteErrorListRow(Long rowId, EntityManager em) {
//        ErrorListRow row = em.find(ErrorListRow.class, rowId);
//        if (row.hasFile())
//            deleteFile(row.getFile());
//        Query query = em.createQuery("delete from ErrorListRow r where r.id = ?1");
//        query.setParameter(1, rowId);
//        em.getTransaction().begin();
//        query.executeUpdate();
//        em.getTransaction().commit();
//    }
//
//    /**
//     * Удаление строки-замечания ЛР вместе с прикрепленным файлом
//     *
//     * @param rowId ид строки
//     */
//    public static void deleteRecommendationRow(Long rowId, EntityManager em) {
//        Recommendation row = em.find(Recommendation.class, rowId);
//        if (row.hasFile())
//            deleteFile(row.getFile());
//        Query query = em.createQuery("delete from recommendation r where r.id = ?1");
//        query.setParameter(1, rowId);
//        em.getTransaction().begin();
//        query.executeUpdate();
//        em.getTransaction().commit();
//    }
//
//    /**
//     * Удаление файла, прикрепленного к строке замечания ЛН
//     * Корректно находит файлы в release-версии, выложенной на tomcat, но не находит в dev-mode
//     *
//     * @param filename errorListRow.getFile()
//     */
//    private static void deleteFile(String filename) {
//        String fullname = System.getProperty("user.dir") + File.separator + "../webapps/uploadErrorListFiles/" + filename;
//        Path path = Paths.get(fullname);
//        path = path.normalize();
//        path.toFile().delete();
//    }
//
//    public static void copyErrorList(Long historyIdSource, Long historyIdDestination, EntityManager em) {
//        Query sourceQuery = em.createQuery("from History h where h.id = ?1");
//        sourceQuery.setParameter(1, historyIdSource);
//        Query destinationQuery = em.createQuery("from History h where h.id = ?1");
//        destinationQuery.setParameter(1, historyIdDestination);
//        History sourceHistory = (History) sourceQuery.getSingleResult();
//        History destinationHistory = (History) destinationQuery.getSingleResult();
//        List<ErrorListRow> sourceRows = sourceHistory.getErrorListRows(); // на самом деле тут список из 1 ErrorList`а
//
//        if (!sourceRows.isEmpty()) {
//            List<ErrorListRow> destinationRows = new ArrayList<>();
//            try {
//                for (ErrorListRow row : sourceRows) {
//                    if (row.getStatusFk() != ErrorListRowStatus.TRANSFORMED_TO_RECOMMENDATION &&
//                            row.getStatusFk() != ErrorListRowStatus.FIX_ACCEPTED) {
//                        ErrorListRow r = new ErrorListRow();
//                        r.setDocumentCodeFk(row.getDocumentCodeFk());
//                        r.setFile(row.getFile());
//                        r.setRemarkTypeFk(row.getRemarkTypeFk());
//                        r.setSummary(row.getSummary());
//                        r.setReason(row.getReason());
//                        r.setStatusFk(row.getStatusFk());
//                        destinationRows.add(r);
//                    }
//                }
//                saveRowsOfList(destinationHistory.getId(), destinationRows, ErrorListRow.class, em);
//            } catch (Exception e) {
//                LOGGER.log(Level.SEVERE, "Произошла ошибка при копировании ЛН", e);
//            }
//        }
//    }
//
//    public static void copyRecommendationList(Long historyIdSource, Long historyIdDestination, EntityManager em) {
//        Query sourceQuery = em.createQuery("from History h where h.id = ?1");
//        sourceQuery.setParameter(1, historyIdSource);
//        Query destinationQuery = em.createQuery("from History h where h.id = ?1");
//        destinationQuery.setParameter(1, historyIdDestination);
//        History sourceHistory = (History) sourceQuery.getSingleResult();
//        History destinationHistory = (History) destinationQuery.getSingleResult();
//
//        if (!sourceHistory.getRecommendationList().isEmpty()) {
//            List<Recommendation> sourceRows = sourceHistory.getRecommendationList();
//            List<Recommendation> destinationRows = new ArrayList<>();
//            try {
//                for (Recommendation row : sourceRows) {
//                    Recommendation r = new Recommendation();
//                    r.setDocumentCodeFk(row.getDocumentCodeFk());
//                    r.setFile(row.getFile());
//                    r.setRemarkTypeFk(row.getRemarkTypeFk());
//                    r.setSummary(row.getSummary());
//                    r.setNumber(row.getNumber());
//                    r.setAddedBy(row.getAddedBy());
//                    destinationRows.add(r);
//                }
//                saveRowsOfList(destinationHistory.getId(), destinationRows, Recommendation.class, em);
//            } catch (Exception e) {
//                LOGGER.log(Level.SEVERE, "Произошла ошибка при копировании ЛР", e);
//            }
//        }
//    }
//
//    /**
//     * Добавление и обновление строк ЛН, относящихся к записи истории. Удаление строк нужно делать отдельно
//     *
//     * @param historyId ид истории
//     * @param rows      обновленный список строк
//     */
//    public static <T extends CommonRowOfList> void saveRowsOfList(Long historyId, List<T> rows, Class<T> clazz, EntityManager em) {
//        saveRejectReasonsForRowsOfList(rows, em);
//        History history = em.find(History.class, historyId);
//        em.getTransaction().begin();
//        if (clazz == ErrorListRow.class) {
//            for (T row : rows) {
//                ((ErrorListRow) row).setHistoryFk(history);
//                em.merge(row);
//            }
//            em.getTransaction().commit();
//            LOGGER.info("Сохранение ЛР:" + "Сохранено " + rows.size() + " замечаний к записи истории: " + history.toString());
//        } else if (clazz == Recommendation.class) {
//            for (T row : rows) {
//                ((Recommendation) row).setHistoryFk(history);
//                if (row.getId() != null) {
//                    em.merge(row);
//                } else {
//                    ((Recommendation) row).setNumber(createNumberForRecommendation(rows, (Recommendation) row, historyId, em));
//                    em.persist(row);
//                }
//            }
//            em.getTransaction().commit();
//            LOGGER.info("Сохранение ЛН:" + "Сохранено " + rows.size() + " замечаний к записи истории: " + history.toString());
//        } else {
//            em.getTransaction().rollback();
//            throw new UnsupportedOperationException("Нет реализации для заданного типа");
//        }
//
//    }
//
//    /**
//     * Находит первую часть ЛР.
//     * <p>
//     * Первая часть ЛР = кол-во историй в которых есть ЛР до заданной истории
//     *
//     * @return первая часть номера
//     */
//    public static String generateFirstPartOfNumberForRecommendation(Long historyId, EntityManager entityManager) {
//        History history = entityManager.find(History.class, historyId);
//        List<History> historyList = history.getDocumentFk().getHistoryList();
//        DataUtils.sortHistoryListByDateDesc(historyList);
//
//        int firstPartOfNumber = 1;
//        for (History h : historyList) {
//            if (h.getRecommendationList() != null && !h.getRecommendationList().isEmpty() && !h.equals(history)) {
//                firstPartOfNumber++;
//            }
//            if (h.equals(history)) {
//                break;
//            }
//        }
//
//        return firstPartOfNumber + "";
//    }
//
//    public static String createNumberForRecommendation(List<? extends CommonRowOfList> recommendations, Recommendation recommendation,
//                                                       Long historyId, EntityManager entityManager) {
//        String listNumber = generateFirstPartOfNumberForRecommendation(historyId, entityManager);
//        History history = entityManager.find(History.class, historyId);
//        String recommendationNumber = "." + (recommendations.indexOf(recommendation) + history.getRecommendationList().size() + 1);
//        return listNumber + recommendationNumber;
//    }
//
//    private static <T extends CommonRowOfList> void saveRejectReasonsForRowsOfList(List<T> rows, EntityManager entityManager) {
//        List<RejectReason> rejectReasonsToSave = new ArrayList<>();
//        for (T row : rows) {
//            if (row.getRejectReasonFk() != null && row.getRejectReasonFk().getId() == null) {
//                rejectReasonsToSave.add(row.getRejectReasonFk());
//            }
//        }
//        entityManager.getTransaction().begin();
//        for (RejectReason rejectReason : rejectReasonsToSave) {
//            entityManager.persist(rejectReason);
//        }
//        entityManager.getTransaction().commit();
//    }
//
//    /**
//     * Считает кол-во рабочих дней (исключая субботу и воскресенье) между двумя датами
//     */
//    public static double getWorkingDaysBetweenTwoDates(Date a, Date b) {
//        Date startDate = new Date(a.getTime());
//        Date endDate = new Date(b.getTime());
//        Calendar startCal = Calendar.getInstance();
//        startCal.setTime(startDate);
//
//        Calendar endCal = Calendar.getInstance();
//        endCal.setTime(endDate);
//
//        int workDays = 0;
//
//        //Return 0 if start and end are the same
//        if (startCal.getTimeInMillis() == endCal.getTimeInMillis()) {
//            return 0.0;
//        }
//
//        if (startCal.getTimeInMillis() > endCal.getTimeInMillis()) {
//            startCal.setTime(endDate);
//            endCal.setTime(startDate);
//        }
//        checkAndSetWorkTime(startDate);
//        checkAndSetWorkTime(endDate);
//
//        Long resMSecs;
//        List<Date> dateList = new ArrayList<>();
//        if (DateUtils.isSameDay(startDate, endDate)) {
//            resMSecs = getTimeTo(startDate, endDate);
//        } else {
//            Calendar tempCal = Calendar.getInstance();
//            tempCal.setTime(startDate);
//            while (!DateUtils.isSameDay(tempCal, endCal)) {
//                if (tempCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && tempCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
//                    ++workDays;
//                    dateList.add(tempCal.getTime());
//                }
//                tempCal.setTime(DateUtils.addDays(tempCal.getTime(), 1));
//            }
//            dateList.add(endCal.getTime());
//            resMSecs = getTimeMsByList(dateList);
//        }
//
//        return countWorkDays(resMSecs);
//    }
//
//    /**
//     * Считает рабочие дни с точностью до десятых (0.1) из расчета
//     * времени рабочего дня 8 часов
//     *
//     * @param res
//     * @return
//     */
//    private static double countWorkDays(long res) {
//        double countOfWorkHours = 8.0;
//        Locale currentLocale = Locale.getDefault();
//        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(currentLocale);
//        otherSymbols.setDecimalSeparator('.');
//        DecimalFormat oneDigit = new DecimalFormat("#.#", otherSymbols); //format to 1 decimal place
//        double resD = res / 1000.0 / 60.0 / 60.0 / countOfWorkHours;
//        resD = Double.parseDouble(oneDigit.format(resD));
//        return resD;
//    }
//
//    /**
//     * Выдает разницу между 2-ым параметром и 1-ым в миллисекундах
//     *
//     * @param from 1-ая дата
//     * @param to   2-ая
//     * @return разница в миллисекундах
//     */
//    private static long getTimeTo(Date from, Date to) {
//        if (to.getTime() <= from.getTime()) {
//            return 0;
//        }
//        return to.getTime() - from.getTime();
//    }
//
//    public static java.sql.Date dayPlusWorkHours(java.sql.Date date, Double workHours) {
//        if (workHours == 0 || date == null || date == new java.sql.Date(0)) {
//            return null;
//        } else {
//            // копируем, чтобы не затереть входные параметры
//            java.sql.Date copyDate = new java.sql.Date(date.getTime());
//            // проверяем и назначаем корректное время входной даты
//            checkAndSetWorkTime(copyDate);
//            // считаем полные рабочие дни
//            int fullWorkDays = (int) (workHours % 8);
//            // считаем остаток
//            Double remainderHours = workHours % 8;
//
//            long msInHour = 60 * 60 * 1000;
//
//            Calendar tempCal = Calendar.getInstance();
//            tempCal.setTime(copyDate);
//            // будем вносить в список даты рабочих дней, пока не добавим нужное количество (fullWorkDays)
//            ArrayList<Date> dateList = new ArrayList<>();
//            while (fullWorkDays > 0) {
//                if (tempCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && tempCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
//                    fullWorkDays--;
//                    dateList.add(tempCal.getTime());
//                }
//            }
//            // берем последний добавленный день, к нему нужно добавить остаток от деления
//            java.sql.Date tempRes = new java.sql.Date(dateList.get(dateList.size() - 1).getTime() + (long) (remainderHours * msInHour));
//            // пробуем привести предварительный результат в рабочий график
//            java.sql.Date tempInWorkTime = new java.sql.Date(tempRes.getTime());
//            checkAndSetWorkTime(tempInWorkTime);
//            // check if tempRes in the same workday
//            long diff = getTimeTo(tempRes, tempInWorkTime);
//            if (diff > 0) {
//                // разница менее 8 часов, добавляем к старту следующего рабочего дня
//                java.sql.Date nextDay = new java.sql.Date(tempRes.getTime());
//                setTimeToStartOfWorkDay(nextDay);
//                tempRes.setTime(nextDay.getTime() + diff * msInHour);
//            }
//            // TODO TEST!
//            return tempRes;
//        }
//    }
//
//
//    /**
//     * Проверяет дату на вхождение в "рабочее время" (9:00 - 17:30)
//     * При несоответствии промежутку заменяет на ближайшую к рабочему времени
//     *
//     * @param date исходная дата
//     */
//    private static void checkAndSetWorkTime(Date date) {
//        if (date.getHours() < 9) {
//            setTimeToStartOfWorkDay(date);
//        } else {
//            if (date.getHours() > 17 || (date.getHours() == 17 && date.getMinutes() > 30)) {
//                setTimeToEndOfWorkDay(date);
//            }
//        }
//    }
//
//    /**
//     * Выставление начала рабочего дня
//     * 09:00
//     *
//     * @param date
//     */
//    private static void setTimeToStartOfWorkDay(Date date) {
//        date.setHours(9);
//        date.setMinutes(0);
//        date.setSeconds(0);
//    }
//
//    /**
//     * Выставление конца рабочего дня
//     * 17:30
//     *
//     * @param date
//     */
//    private static void setTimeToEndOfWorkDay(Date date) {
//        date.setHours(17);
//        date.setMinutes(30);
//        date.setSeconds(0);
//    }
//
//    /**
//     * Считает миллисекунды рабочего времени во входном списке
//     *
//     * @param dateList список дат
//     * @return рабочее время в миллисекундах
//     */
//    private static long getTimeMsByList(List<Date> dateList) {
//        long res = 0;
//        res += getTimeForFirstAndLast(dateList);
//        if (dateList.size() > 2) {
//            // more than two days
//            int workHours = 8;
//            res += TimeUnit.HOURS.toMillis(workHours) * (dateList.size() - 2);
//        }
//        return res;
//    }
//
//    /**
//     * Время для первого и последнего дня, обед не учитывается,
//     * фактически рабочий день расширяется на полчаса
//     *
//     * @param dateList - список дат
//     * @return (long) миллисекунды, потраченные в первый и последний рабочий день
//     */
//    private static long getTimeForFirstAndLast(List<Date> dateList) {
//        long res = 0;
//        Date firstEndOfTheDay = new Date(dateList.get(0).getTime());
//        setTimeToEndOfWorkDay(firstEndOfTheDay);
//        res += getTimeTo(dateList.get(0), firstEndOfTheDay);
//
//        Date lastDate = dateList.get(dateList.size() - 1);
//        Date lastStartOfTheDay = new Date(lastDate.getTime());
//        setTimeToStartOfWorkDay(lastStartOfTheDay);
//        res += getTimeTo(lastStartOfTheDay, lastDate);
//        return res;
//    }
//}
