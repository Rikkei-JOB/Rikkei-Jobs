package com.rikkei.jobs.company.repository;

import com.rikkei.jobs.company.entity.JobSkill;
import com.rikkei.jobs.company.entity.JobSkillId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobSkillRepository extends JpaRepository<JobSkill, JobSkillId> {

    List<JobSkill> findByJobId(UUID jobId);

    /** Mỗi dòng là [jobId, số kỹ năng bắt buộc]; JD không có kỹ năng bắt buộc sẽ không xuất hiện. */
    @Query("select s.jobId, count(s) from JobSkill s where s.jobId in :ids and s.required = true group by s.jobId")
    List<Object[]> countRequiredByJobIds(@Param("ids") Collection<UUID> ids);

    /**
     * Xoá hàng loạt: flush thay đổi đang chờ trước khi chạy. Không tự clear context (sẽ tách cả Job đang sửa),
     * nên nơi gọi không được nạp JobSkill của JD này trước khi xoá rồi chèn lại cùng khoá.
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from JobSkill s where s.jobId = :jobId")
    void deleteByJobId(@Param("jobId") UUID jobId);
}
