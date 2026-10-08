package com.gatelms.repository

import com.gatelms.entity.Pass1Status
import com.gatelms.entity.Pass2Status
import com.gatelms.entity.QuestionProgress
import com.gatelms.entity.QuestionProgressId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface QuestionProgressRepository : JpaRepository<QuestionProgress, QuestionProgressId> {
    fun findByIdUserIdAndIdQuestionId(userId: UUID, questionId: UUID): Optional<QuestionProgress>
    fun findAllByIdUserId(userId: UUID): List<QuestionProgress>
    fun countByIdUserIdAndPass1Status(userId: UUID, status: Pass1Status): Long
    fun countByIdUserIdAndPass2Status(userId: UUID, status: Pass2Status): Long
    fun countByIdUserIdAndIsBookmarkedTrue(userId: UUID): Long

    @Query("SELECT qp.id.questionId FROM QuestionProgress qp WHERE qp.id.userId = :userId AND qp.isBookmarked = true")
    fun findBookmarkedQuestionIds(@Param("userId") userId: UUID): List<UUID>

    @Query("SELECT qp FROM QuestionProgress qp WHERE qp.id.userId = :userId AND qp.id.questionId IN :questionIds")
    fun findAllByUserIdAndQuestionIdIn(@Param("userId") userId: UUID, @Param("questionIds") questionIds: Collection<UUID>): List<QuestionProgress>

    @Query("""
        SELECT q.topic.id, count(qp) 
        FROM QuestionProgress qp 
        JOIN Question q ON q.id = qp.id.questionId
        WHERE qp.id.userId = :userId 
          AND qp.pass1Status IN ('SOLVED', 'ASSISTED')
          AND q.curationStatus <> 'REJECTED'
          AND q.topic.id IS NOT NULL
        GROUP BY q.topic.id
    """)
    fun countSolvedByTopicForUser(@Param("userId") userId: UUID): List<Array<Any>>

    @Query("""
        SELECT q.subject.id, count(qp) 
        FROM QuestionProgress qp 
        JOIN Question q ON q.id = qp.id.questionId
        WHERE qp.id.userId = :userId 
          AND qp.pass1Status IN ('SOLVED', 'ASSISTED')
          AND q.curationStatus <> 'REJECTED'
          AND q.subject.id IS NOT NULL
        GROUP BY q.subject.id
    """)
    fun countSolvedBySubjectForUser(@Param("userId") userId: UUID): List<Array<Any>>

    fun findFirstByIdUserIdAndLastAttemptedAtIsNotNullOrderByLastAttemptedAtDesc(userId: UUID): Optional<QuestionProgress>
}
