package cn.nbbandxdd.survey.resprec.repository;

import cn.nbbandxdd.survey.resprec.repository.entity.ExamStatEntity;
import cn.nbbandxdd.survey.resprec.repository.entity.GrpStatEntity;
import cn.nbbandxdd.survey.resprec.repository.entity.RespRecEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>作答记录Repository。
 *
 * @author howcurious
 */
public interface RespRecRepository extends ReactiveCrudRepository<RespRecEntity, String> {

    @Modifying
    @Query("INSERT INTO RESP_REC (OPEN_ID, EXAM_CD, SCRE, SPND, DAT) VALUES (:#{#entity.openId}, :#{#entity.examCd}, :#{#entity.scre}, :#{#entity.spnd}, :#{#entity.dat})")
    Mono<Integer> insert(RespRecEntity entity);

    @Modifying
    @Query("DELETE FROM RESP_REC WHERE OPEN_ID = :openId AND EXAM_CD = :examCd")
    Mono<Integer> deleteByOpenIdAndExamCd(String openId, String examCd);

    @Modifying
    @Query("DELETE FROM RESP_REC REC WHERE EXISTS (SELECT 1 FROM EXAM WHERE REC.EXAM_CD = EXAM.EXAM_CD AND EXAM.EXAM_CD = :examCd AND EXAM.LAST_MANT_USR = :lastMantUsr)")
    Mono<Integer> deleteByExamCdAndLastMantUsr(String examCd, String lastMantUsr);

    @Query("SELECT * FROM RESP_REC WHERE OPEN_ID = :openId AND EXAM_CD = :examCd")
    Mono<RespRecEntity> findByOpenIdAndExamCd(String openId, String examCd);

    @Query("(" +
        "SELECT OPEN_ID, EXAM_CD, SCRE, SPND, DAT " +
        "FROM RESP_REC REC " +
        "WHERE OPEN_ID = :openId AND EXISTS (" +
            "SELECT 1 FROM EXAM " +
            "WHERE EXAM.EXAM_CD = REC.EXAM_CD AND " +
            "EXAM.ANSW_IMM_IND = '1'" +
            ") " +

        "UNION " +

        "SELECT OPEN_ID, EXAM_CD, SCRE, SPND, DAT " +
        "FROM RESP_REC REC " +
        "WHERE OPEN_ID = :openId AND EXISTS (" +
            "SELECT 1 FROM EXAM " +
            "WHERE EXAM.EXAM_CD = REC.EXAM_CD AND " +
            "EXAM.ANSW_IMM_IND = '0' AND EXAM.END_TIME < NOW()" +
            ") " +
        ")" +
        "ORDER BY DAT DESC, EXAM_CD DESC LIMIT :limit OFFSET :offset")
    Flux<RespRecEntity> findListByOpenId(String openId, Integer limit, Integer offset);

    @Query("SELECT OPEN_ID, EXAM_CD, SCRE, SPND, DAT " +
        "FROM RESP_REC REC " +
        "WHERE EXISTS (" +
            "SELECT 1 FROM EXAM " +
            "WHERE REC.EXAM_CD = EXAM.EXAM_CD " +
            "AND EXAM.EXAM_CD = :examCd " +
            "AND EXAM.LAST_MANT_USR = :lastMantUsr" +
            ") " +
        "ORDER BY SCRE DESC, SPND LIMIT :limit OFFSET :offset")
    Flux<RespRecEntity> findRankByExamCdAndLastMantUsr(String examCd, String lastMantUsr, Integer limit, Integer offset);

    @Query("SELECT :examCd AS EXAM_CD, GRP.DPRT_NAM AS DPRT_NAM, GRP.GRP_NAM AS GRP_NAM, " +
            "IFNULL(RES.CNT, 0) AS CNT, " +
            "IFNULL(TOT.TOT_CNT, 0) AS TOT_CNT, " +
            "(CASE WHEN RES.CNT IS NULL OR TOT.TOT_CNT IS NULL OR TOT.TOT_CNT = 0 THEN 0 ELSE RES.CNT / TOT.TOT_CNT END) AS PTPN_RATE, " +
            "IFNULL(RES.AVG_SCRE, 0) AS AVG_SCRE, " +
            "IFNULL(RES.AVG_SPND, 0) AS AVG_SPND " +
        "FROM (SELECT * FROM GRP_INFO WHERE DPRT_NAM = :dprtNam) GRP " +
        "LEFT JOIN (" +
            "SELECT USR.DPRT_NAM AS DPRT_NAM, USR.GRP_NAM AS GRP_NAM, " +
                "COUNT(1) AS CNT, AVG(REC.SCRE) AS AVG_SCRE, AVG(REC.SPND) AS AVG_SPND " +
            "FROM (SELECT * FROM RESP_REC WHERE EXAM_CD = :examCd) REC " +
            "INNER JOIN (SELECT * FROM USR_INFO WHERE DPRT_NAM = :dprtNam) USR ON REC.OPEN_ID = USR.OPEN_ID " +
            "GROUP BY USR.DPRT_NAM, USR.GRP_NAM" +
        ") RES ON GRP.DPRT_NAM = RES.DPRT_NAM AND GRP.GRP_NAM = RES.GRP_NAM " +
        "LEFT JOIN (SELECT DPRT_NAM, GRP_NAM, COUNT(1) AS TOT_CNT FROM USR_INFO GROUP BY DPRT_NAM, GRP_NAM" +
        ") TOT ON GRP.DPRT_NAM = TOT.DPRT_NAM AND GRP.GRP_NAM = TOT.GRP_NAM " +
        "ORDER BY GRP.SEQ_NO")
    Flux<GrpStatEntity> findGrpStatByExamCdAndDprtNam(String examCd, String dprtNam);

    @Query("SELECT REC.EXAM_CD AS EXAM_CD, EXAM.TTL AS TTL, " +
        "COUNT(1) AS CNT, AVG(SCRE) AS AVG_SCRE, AVG(SPND) AS AVG_SPND, " +
        "SUM(CASE WHEN SCRE BETWEEN 0 AND 40 THEN 1 ELSE 0 END) AS CNT_U40, " +
        "SUM(CASE WHEN SCRE BETWEEN 0 AND 40 THEN 1 ELSE 0 END) / COUNT(1) AS RATE_U40, " +
        "SUM(CASE WHEN SCRE BETWEEN 41 AND 70 THEN 1 ELSE 0 END) AS CNT_U70, " +
        "SUM(CASE WHEN SCRE BETWEEN 41 AND 70 THEN 1 ELSE 0 END) / COUNT(1) AS RATE_U70, " +
        "SUM(CASE WHEN SCRE BETWEEN 71 AND 100 THEN 1 ELSE 0 END) AS CNT_U100, " +
        "SUM(CASE WHEN SCRE BETWEEN 71 AND 100 THEN 1 ELSE 0 END) / COUNT(1) AS RATE_U100, " +
        "SUM(CASE WHEN SCRE = 100 THEN 1 ELSE 0 END) AS CNT_100, " +
        "SUM(CASE WHEN SCRE = 100 THEN 1 ELSE 0 END) / COUNT(1) AS RATE_100 " +
        "FROM RESP_REC REC LEFT JOIN EXAM ON REC.EXAM_CD = EXAM.EXAM_CD " +
        "WHERE REC.EXAM_CD = :examCd")
    Mono<ExamStatEntity> findExamStatByExamCd(String examCd);
}
