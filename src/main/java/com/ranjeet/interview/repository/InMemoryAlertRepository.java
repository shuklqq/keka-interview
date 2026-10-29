package com.ranjeet.interview.repository;

import com.ranjeet.interview.entity.Alert;
import com.ranjeet.interview.entity.IncidentStatus;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryAlertRepository extends InMemoryRepository<Alert, String> implements AlertRepository {

    public InMemoryAlertRepository() {
        super("Alert", Alert::getId);
    }

    @Override
    public List<Alert> findByStatus(IncidentStatus status) {
        return stream().filter(alert -> alert.getStatus() == status).toList();
    }
}
