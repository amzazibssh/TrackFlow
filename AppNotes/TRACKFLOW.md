# TrackFlow

Logistics platform built as the main application of a fictional courier company, **TrackFlow Express**. It manages shipments end to end, from creation to delivery, through an event-driven microservices architecture.

## Context

TrackFlow Express is an imaginary regional courier operating across Morocco, serving individuals and businesses. It offers Standard, Express and Cash on Delivery (COD) services through a network of regional hubs and a fleet of drivers.

Since there is no real client, requirements are derived from the logistics domain and from the behavior of real carriers (tracking flows, delivery attempts, incidents, invoicing, COD reconciliation).

## Users

| Role | Main capabilities |
|---|---|
| Customer | Create a shipment, get a price quote, track a parcel |
| Business customer | Bulk import, API access, webhooks, monthly invoices, COD payouts |
| Dispatcher | Assign shipments to drivers, rebalance routes, handle incidents |
| Hub operator | Scan parcels in and out, sort, manage hub inventory |
| Driver | Daily route, status updates, proof of delivery, COD collection |
| Finance | Invoices, COD reconciliation, disputes, refunds |
| Admin | Users, roles, pricing rules, zones, hubs, audit logs |

## Tech stack

- Backend: Java 21, Spring Boot, Spring Data JPA, Spring Security (JWT)
- Messaging: Apache Kafka
- Databases: Oracle (business data), MongoDB (tracking and events)
- Frontend: React, Material UI
- Infrastructure: Docker Compose, GitLab CI/CD, AWS

## Architecture

Services communicate through Kafka events. The shipment service is the source of truth for the shipment lifecycle. Other services react to its events.

```
shipment-service --(shipment.created, shipment.status-changed)--> Kafka
                                                                    |
                                  +---------------------------------+
                                  v                                 v
                           tracking-service                 notification-service
                                                                    |
                                                  (notification.sent / failed)
```

### Event format

```json
{
  "eventId": "uuid",
  "type": "shipment.status-changed",
  "occurredAt": "2026-10-14T10:00:00Z",
  "trackingNumber": "TF-2026-000123",
  "from": "PICKED_UP",
  "to": "IN_TRANSIT",
  "location": "Marrakech Hub"
}
```

Consumers must be idempotent, using `eventId` to ignore replayed events.

## Shipment lifecycle

```
CREATED -> PICKED_UP -> IN_TRANSIT -> OUT_FOR_DELIVERY -> DELIVERED
CREATED -> CANCELED
PICKED_UP / IN_TRANSIT / OUT_FOR_DELIVERY -> FAILED
FAILED -> OUT_FOR_DELIVERY (retry) or RETURNED
```

Terminal states: `DELIVERED`, `CANCELED`, `RETURNED`. Cancellation is only allowed before pickup. Invalid transitions are rejected with HTTP 409.

## Services

### Phase 1

#### shipment-service
- Role: source of truth for shipments and their lifecycle.
- Owns: `Shipment` entity, state machine, status history (Oracle).
- Exposes: `POST /shipments`, `GET /shipments/{trackingNumber}`, `POST /shipments/{id}/status`.
- Publishes: `shipment.created`, `shipment.status-changed`.
- Rules: transitions are enforced in the domain model, every transition is stored in a status history table, optimistic locking on updates.

#### tracking-service
- Role: builds and serves the tracking timeline of each parcel.
- Owns: event timeline per tracking number (MongoDB).
- Consumes: `shipment.*`.
- Exposes: public tracking API by tracking number.
- Rules: idempotent consumption based on `eventId`.

#### notification-service
- Role: informs recipients of shipment progress.
- Owns: message log and templates.
- Consumes: `shipment.status-changed`.
- Publishes: `notification.sent`, `notification.failed`.
- Channels: email first, SMS later.

### Phase 2

####  identity-service
- Role: users, roles and authentication.
- Provides: JWT issuance, role-based access (customer, admin, dispatcher, driver, finance).

#### pricing-service
- Role: computes shipment prices.
- Inputs: zones, weight, distance, service level.

### Phase 3

#### dispatch-service
- Role: driver assignment and route batching.

#### billing-service
- Role: invoices and COD reconciliation.
- Trigger: delivery events.

### Phase 4

#### incident-service
- Role: manages delays, damage and address problems.
- Features: SLA timers, escalation.

#### Engineering showcases
- Saga pattern for create shipment, price, invoice, notify, with compensation on failure.
- Outbox pattern for reliable event publishing.
- Dead-letter topics for failed consumers.
- Observability: Micrometer, Prometheus, Grafana, correlation IDs.
- Webhooks for third-party integrations.

## Frontends

- Public tracking page and customer portal.
- Back-office for admin, dispatcher and finance.
- Driver mobile-first view (PWA).

## Roadmap

| Phase | Scope | Goal |
|---|---|---|
| 1 | shipment, tracking, notification | Prove the Kafka flow end to end |
| 2 | identity, pricing | Roles and automatic pricing |
| 3 | dispatch, billing | Operations and money flow |
| 4 | incident and showcases | Resilience and observability |

Rule: a phase starts only when the previous one runs through Docker Compose.
