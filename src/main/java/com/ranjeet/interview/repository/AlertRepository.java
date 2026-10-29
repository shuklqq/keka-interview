package com.ranjeet.interview.repository;

import com.ranjeet.interview.entity.Alert;
import com.ranjeet.interview.entity.IncidentStatus;
import java.util.List;

public interface AlertRepository extends BaseRepository<Alert, String> {

    List<Alert> findByStatus(IncidentStatus status);
}
