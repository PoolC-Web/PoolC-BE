package org.poolc.api.member.repository;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.MutablePair;
import org.poolc.api.member.domain.Member;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MemberQueryRepository {

    private static final String RECOGNIZED_SESSION_HOURS_SQL =
            "select host_member.login_id as login_id, a.id as activity_id, a.title, true as hosted, sum(s.hour * 2.5) as recognized_hours " +
                    "from session s join activity a on s.activity_id = a.id join member host_member on a.host = host_member.uuid " +
                    "where a.start_date between :semesterStartDate and :semesterEndDate and s.date <= :asOfDate " +
                    "group by host_member.login_id, a.id, a.title " +
                    "union all " +
                    "select attendance.member_loginid as login_id, a.id as activity_id, a.title, false as hosted, sum(s.hour) as recognized_hours " +
                    "from attendance join session s on attendance.session_id = s.id " +
                    "join activity a on s.activity_id = a.id " +
                    "where a.start_date between :semesterStartDate and :semesterEndDate and s.date <= :asOfDate " +
                    "and attendance.member_loginid <> a.host " +
                    "group by attendance.member_loginid, a.id, a.title";

    private static final String RECOGNIZED_PROJECT_HOURS_SQL =
            "select project_members.member_loginids as login_id, project.id as project_id, project.name, 10 as recognized_hours " +
                    "from project_members join project on project_members.project_id = project.id " +
                    "where project.start_date between :semesterStartDate and :semesterEndDate";

    private static final String RECOGNIZED_SESSION_TOTALS_SQL =
            "select login_id, sum(recognized_hours) as recognized_hours from (" +
                    RECOGNIZED_SESSION_HOURS_SQL +
                    ") recognized_session_rows group by login_id";

    private static final String RECOGNIZED_PROJECT_TOTALS_SQL =
            "select login_id, sum(recognized_hours) as recognized_hours from (" +
                    RECOGNIZED_PROJECT_HOURS_SQL +
                    ") recognized_project_rows group by login_id";

    private static final String RECOGNIZED_OFFICIAL_ACTIVITY_HOURS_SQL =
                    "select official_activity_members.member_login_id as login_id, official_activity.id as activity_id, official_activity.title, " +
                    "official_activity.recognized_hours " +
                    "from official_activity_members join official_activity on official_activity_members.activity_id = official_activity.id " +
                    "where official_activity.activity_date between :semesterStartDate and :semesterEndDate";

    private static final String RECOGNIZED_OFFICIAL_ACTIVITY_TOTALS_SQL =
                    "select official_activity_members.member_login_id as login_id, official_activity.recognized_hours " +
                    "from official_activity_members join official_activity on official_activity_members.activity_id = official_activity.id " +
                    "where official_activity.activity_date between :semesterStartDate and :semesterEndDate";

    private final EntityManager em;

    public List<MutablePair<String, Long>> getHours(LocalDate startDate, LocalDate endDate) {
        List<Object[]> list = em.createNativeQuery("select attendance.member_loginid, sum(session.hour) from attendance join session on attendance.session_id=session.id join activity on session.activity_id=activity.id where activity.start_date between :startDate and :endDate group by attendance.member_loginid")
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();
        return list.stream()
                .map(s -> new MutablePair<String, Long>(s[0].toString(), Long.parseLong(s[1].toString())))
                .collect(Collectors.toList());
    }

    public Map<String, BigDecimal> getRecognizedHours(LocalDate semesterStartDate, LocalDate semesterEndDate,
                                                        LocalDate asOfDate) {
        List<Object[]> rows = em.createNativeQuery(
                "select login_id, sum(recognized_hours) from (" + RECOGNIZED_SESSION_TOTALS_SQL +
                        " union all " + RECOGNIZED_PROJECT_TOTALS_SQL +
                        " union all " + RECOGNIZED_OFFICIAL_ACTIVITY_TOTALS_SQL +
                        ") recognized_hour_rows group by login_id"
        ).setParameter("semesterStartDate", semesterStartDate)
                .setParameter("semesterEndDate", semesterEndDate)
                .setParameter("asOfDate", asOfDate)
                .getResultList();

        Map<String, BigDecimal> hoursByLoginId = new HashMap<>();
        for (Object[] row : rows) {
            hoursByLoginId.put(row[0].toString(), new BigDecimal(row[1].toString()));
        }
        return hoursByLoginId;
    }

    public List<RecognizedActivityHours> getRecognizedActivityHours(String loginId, LocalDate semesterStartDate,
                                                                      LocalDate semesterEndDate, LocalDate asOfDate) {
        List<Object[]> rows = em.createNativeQuery(
                "select activity_id, title, hosted, recognized_hours from (" + RECOGNIZED_SESSION_HOURS_SQL +
                        ") recognized_session_rows where login_id = :loginId order by title"
        ).setParameter("loginId", loginId)
                .setParameter("semesterStartDate", semesterStartDate)
                .setParameter("semesterEndDate", semesterEndDate)
                .setParameter("asOfDate", asOfDate)
                .getResultList();
        return rows.stream()
                .map(row -> new RecognizedActivityHours(((Number) row[0]).longValue(), row[1].toString(),
                        new BigDecimal(row[3].toString()), (Boolean) row[2]))
                .collect(Collectors.toList());
    }

    public List<RecognizedProjectHours> getRecognizedProjectHours(String loginId, LocalDate semesterStartDate,
                                                                    LocalDate semesterEndDate) {
        List<Object[]> rows = em.createNativeQuery(
                "select project_id, name, recognized_hours from (" + RECOGNIZED_PROJECT_HOURS_SQL +
                        ") recognized_project_rows where login_id = :loginId order by name"
        ).setParameter("loginId", loginId)
                .setParameter("semesterStartDate", semesterStartDate)
                .setParameter("semesterEndDate", semesterEndDate)
                .getResultList();
        return rows.stream()
                .map(row -> new RecognizedProjectHours(((Number) row[0]).longValue(), row[1].toString(),
                        new BigDecimal(row[2].toString())))
                .collect(Collectors.toList());
    }

    public List<RecognizedOfficialActivityHours> getRecognizedOfficialActivityHours(String loginId,
                                                                                      LocalDate semesterStartDate,
                                                                                      LocalDate semesterEndDate) {
        List<Object[]> rows = em.createNativeQuery(
                "select activity_id, title, recognized_hours from (" + RECOGNIZED_OFFICIAL_ACTIVITY_HOURS_SQL +
                        ") recognized_official_activity_rows where login_id = :loginId order by activity_id"
        ).setParameter("loginId", loginId)
                .setParameter("semesterStartDate", semesterStartDate)
                .setParameter("semesterEndDate", semesterEndDate)
                .getResultList();
        return rows.stream()
                .map(row -> new RecognizedOfficialActivityHours(((Number) row[0]).longValue(), row[1].toString(),
                        new BigDecimal(row[2].toString())))
                .collect(Collectors.toList());
    }

    public Long getMyHour(Member member, LocalDate startDate, LocalDate endDate){
        Object singleResult = em.createNativeQuery("select sum(session.hour) from attendance join session on attendance.session_id=session.id join activity on session.activity_id=activity.id where attendance.member_loginid = :memberId and activity.start_date between :startDate and :endDate")
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setParameter("memberId", member.getLoginID()).getSingleResult();
        if(singleResult==null){
            return 0L;
        }
        Long firstResult = Long.parseLong(singleResult.toString());
        //refactoring 마렵네;;
        return firstResult;
    }
}
