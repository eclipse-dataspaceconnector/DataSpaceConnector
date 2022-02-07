# Tracing 

## Decision

Use [OpenTelemetry](https://opentelemetry.io/) to enable distributed tracing in EDC. 

[Context propagation](https://opentelemetry.io/docs/instrumentation/java/manual/#context-propagation) needs to be implemented accordingly so that traces are propagated across asynchronous workers. Business entities processed by async workers are used as carriers of tracing information, which is persisted together with the rest of the entity.

## Span naming

Open telemetry spans are named according to the best practices mentioned in the [documentation](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/api.md#span):

```java
@WithSpan(value = "<ACTION>_<OBJECT_RECEIVING_ACTION>")

// e.g.
@WithSpan(value = "save_contract_negotiation")
```