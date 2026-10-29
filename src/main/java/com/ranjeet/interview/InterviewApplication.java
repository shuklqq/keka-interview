package com.ranjeet.interview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/*
Machine Coding Interview — Candidate Brief
Alert & On-Call Notification Service
• 60 minutes
• SE / SSE
1. The problem
You are building the core of a small incident-alerting service, similar in spirit to PagerDuty.
Monitored services emit alerts.
Your service ingests each alert, records it as an incident, and — depending on severity — notifies the on-call person.
For the most severe alerts, “notify” means placing an automated phone call through an external telephony provider (we will use a mocked Exotel-style API).
Build a small HTTP Service around this.
1.1 Severity → notification rules
• CRITICAL → place a phone call via the Exotel mock to the on-call number for that service, with the alert message as the spoken text.
• WARNING → no phone call. Record the incident with notification status SKIPPED (reason: severity below threshold).
• INFO → record the incident only. No notification.
1.2 Seed / configuration data (you may hardcode this)
A static map of serviceId → on-call phone number.
You do not need an API to manage this.
{
  "svc-payments": "+919900112233",
  "svc-checkout": "+919900445566",
  "svc-search": "+919900778899"
}
1.3 APIs to implement
(a) Ingest an alert
POST /alert
Accepts an alert, creates an incident, applies the notification rules above, and returns the created incident.
Request body:
{
  "serviceId": "svc-payments",
  "severity": "CRITICAL",
  "title": "Payment gateway timeout",
  "description": "p99 latency > 5s for 3 minutes",
  "dedupKey": "payments-latency-001",
  "timestamp": "2026-06-09T10:15:30Z"
}
// INFO | WARNING | CRITICAL
(see incident shape in 1.4)
(b) List incidents
GET /incidents?status=TRIGGERED
Returns all incidents, optionally filtered by status.
1.4 Incident shape (what you persist and return)
{
  "incidentId": "inc-1029",
  "serviceId": "svc-payments",
  "severity": "CRITICAL",
  "title": "Payment gateway timeout",
  "description": "p99 latency > 5s for 3 minutes",
  "status": "NOTIFIED",
  "createdAt": "2026-06-09T10:15:31Z",
  "notification": {
    "channel": "PHONE",
    "status": "SENT",
    "providerCallId": "CA-7781",
    "lastError": null
  }
}
// TRIGGERED | NOTIFIED | RESOLVED
// SENT | FAILED | SKIPPED
// from the provider on success, else null
// short reason when FAILED
1.5 The Exotel mock (external phone-call API)
Request
POST {EXOTEL_BASE_URL}/v1/calls
{
  "to": "+919900112233",
  "message": "CRITICAL: Payment gateway timeout on svc-payments"
}
Response
Success — HTTP 200:
{
  "callSid": "CA-7781",
  "status": "queued"
}
2. Scope — start here, then go as far as you can
Core (aim to get this working and clean):
• POST /alert ingesting an alert and creating an incident with the correct notification behaviour per severity.
• The CRITICAL path calling the Exotel mock and recording the outcome (SENT / FAILED) on the incident.
• List Incidents API
• Sensible models, layering, logging, and error handling throughout.
*/
@SpringBootApplication
public class InterviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewApplication.class, args);
    }
}
