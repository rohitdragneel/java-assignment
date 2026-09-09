# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**

In a fulfillment setup, the real headache isn't tracking direct costs—it's figuring out how to fairly distribute shared and indirect expenses.

Direct costs like packing materials or the labor hours spent picking a specific order are relatively easy to assign. But things get messy fast when dealing with shared resources:
- **Shared transportation:** If a single truck leaves a regional warehouse and drops off pallets at three different stores along a route, how do you split that freight bill? Splitting it evenly is unfair if Store A took 80% of the cargo volume while Store C only needed two small boxes. A fair allocation usually requires weighting by volume, weight, or drop-off distance.
- **Facility overhead:** Fixed expenses like warehouse rent, utilities, supervisor salaries, and cold storage power run 24/7 regardless of daily volume. If an urban store experiences a slow week, should its allocated overhead jump artificially?
- **Split shipments:** When a store's stock replenishment has to be split across two warehouses because one ran out of inventory, transportation and handling costs effectively double. Does the receiving store absorb that penalty, or does it sit with the supply chain team as an inventory planning failure?

From an architecture perspective, I'd avoid batch-processing all of this at month-end. Instead, I'd capture costs as operational events happen (e.g., when an order is picked, loaded onto a truck, or delivered). This gives both operations and store managers near real-time visibility into costs rather than having to wait for finance to close the books weeks later.

**Key questions to clarify scope:**
1. What level of granularity does the business actually need to see? (Is high-level cost per store/warehouse enough, or do we need per-SKU and per-order profitability?)
2. How does the company currently handle shared logistics when multiple stores share a single delivery run?
3. Who is financially accountable for damaged, expired, or lost stock—the originating warehouse or the receiving store?

---

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**

In physical distribution, transportation and warehouse floor labor typically eat up 70% to 80% of total operating spend. That is where optimization moves the needle most.

A few practical areas I would target:
- **Smarter order routing:** Route store replenishment to the nearest warehouse that can fulfill the order completely. Avoiding split shipments saves immediate freight and double packaging costs.
- **Pick-path and batch picking in warehouses:** Instead of workers walking back and forth across the entire facility for single store orders, grouping picks by zone or batching orders going to the same delivery run drastically cuts floor travel time.
- **Balancing capacity across locations:** In our system, locations have specific capacity caps. Keeping high-velocity buffer stock in cheaper regional hubs rather than maxing out expensive urban facilities keeps carrying costs under control while still meeting delivery windows.

**How I would prioritize and tackle this:**
I'd start by looking at where the biggest cost leaks are rather than trying to optimize everything at once:
1. *Immediate wins:* Carrier rate shopping, consolidating store delivery schedules (e.g., standardizing delivery days instead of running half-empty trucks on demand).
2. *Medium-term technical improvements:* Intelligent routing in the fulfillment service to automatically favor full-order fulfillment from the closest node.
3. *Strategic initiatives:* Dynamic inventory rebalancing algorithms based on seasonal store demand.

The critical trade-off to watch is customer/store SLA. Cutting shipping costs by choosing a cheaper, slower carrier or holding trucks until they're 100% full looks great on a logistics spreadsheet, but if stores run out of popular products and lose sales, the "saving" is an illusion.

**Key questions to clarify scope:**
1. What is our current breakdown of fulfillment costs—where is the biggest bleeding right now (labor overtime, freight rates, or inventory holding costs)?
2. Are there non-negotiable delivery SLAs or delivery frequency requirements that we must preserve for the stores?

---

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**

Operations and finance often speak completely different languages. Ops cares about pallets moved, truck departure times, and pick rates; finance cares about general ledger accounts, accruals, and month-end margins.

When these systems aren't tightly connected, finance ends up operating on delayed estimates and gets hit by surprise costs—like unscheduled overtime or spot-market freight rate spikes—weeks after the work happened. Integrating the Cost Control Tool with the ERP (e.g., SAP, NetSuite) gives finance genuine real-time visibility into Cost of Goods Sold (COGS).

**How I would design the integration:**
- **Asynchronous, decoupled communication:** Never make direct synchronous HTTP calls to the ERP inside operational workflows. If the accounting system is down for maintenance or running slow, warehouse staff must still be able to scan boxes and dispatch trucks without getting blocked. I'd use an event-driven setup (such as a transactional outbox pattern) where fulfillment events are saved locally and published reliably to a message bus.
- **Strict idempotency:** Financial ledgers cannot tolerate duplicate entries. Every cost event must carry a deterministic transaction key (like `fulfillment_id + event_type`). If a network timeout occurs and a message gets retried, the financial receiver must detect the duplicate and discard it safely.
- **Automated reconciliation:** Even with robust messaging, discrepancies happen over time (network drops, rounding differences, schema updates). I'd build an automated daily reconciliation worker that compares total operational cost events against recorded ledger entries and alerts both teams to discrepancies immediately.

**Key questions to clarify scope:**
1. What interfaces does the current financial/ERP system support (modern REST/webhook APIs, message queues like Kafka/RabbitMQ, or scheduled SFTP batch files)?
2. Is eventual consistency acceptable (e.g., cost records syncing within 5–15 minutes), or are there specific operations that require immediate two-way confirmation?

---

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**

Fulfillment volume is never static—it swings with promotions, holidays, and seasonal customer habits. Without dependable forecasting, warehouses either over-hire temporary labor and burn money on idle shifts, or under-hire, blow delivery deadlines, and end up paying astronomical emergency overtime and expedited carrier fees.

**What I would consider when designing the forecasting system:**
- **Driver-based modeling instead of guessing dollar amounts:** Forecasting shouldn't just be managers guessing future dollar figures. The system should project the actual physical work drivers—expected store sales volume, number of items to pick, required pallet movements, and delivery routes. Once you know the unit volume, you apply known unit cost rates (labor cost per pick, fuel cost per mile) to compute the projected budget.
- **Incorporating seasonality and marketing calendars:** The model needs historical throughput data, but it also must ingest business inputs: planned promotional campaigns, holiday calendars, and planned new store openings.
- **"What-if" scenario simulation:** Give operations and finance teams the ability to test scenarios before finalizing numbers. For example: *"What happens to our quarterly spend if fuel surcharges rise by 12%?"* or *"What if we reroute 5 stores from Warehouse A to Warehouse B?"*

**Key questions to clarify scope:**
1. How often does the organization re-forecast—is it an annual fixed budget, or are we building toward a rolling monthly/quarterly forecast?
2. Looking back at past quarters, what external factors caused the biggest variance between forecast and actual spend (e.g., labor shortages, sudden carrier price hikes, unexpected inventory spikes)?

---

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**

Reusing a `businessUnitCode` while retiring an old warehouse and bringing up a new facility is a common pattern when modernizing infrastructure, but it introduces distinct financial and data challenges.

**Why preserving cost history is essential:**
1. **Auditing and compliance:** Even after a physical facility is closed, financial records—such as asset depreciation on machinery, lease obligations, and previous operational tax deductions—must remain accessible for tax authorities and auditors for several years.
2. **True ROI benchmarking:** If the business invested capital into a new warehouse expecting improved throughput and lower cost per unit, you cannot validate that business case without historical baseline data from the archived facility to compare against.

**Keeping the new warehouse operation within budget:**
- **Isolating transition costs from operational baseline:** Transitioning to a new warehouse involves significant one-off expenses: moving physical inventory, dual-lease overlap, decommissioning old equipment, and training new staff. If these migration costs get lumped into the new facility's recurring operational costs, the new warehouse will falsely look like an unprofitable disaster in its first few months. The system must clearly tag one-off migration costs separately from ongoing OPEX.
- **Clean data modeling with temporal tracking:** Because both the old and new warehouse records share the same `businessUnitCode`, the database records must maintain distinct primary IDs and clear lifecycle timestamps (`createdAt`, `archivedAt`), just as modeled in our domain repository. This ensures financial reporting queries can easily aggregate historical data across the business unit or isolate a specific physical facility's lifespan.

**Key questions to clarify scope:**
1. How should one-time relocation and setup costs be classified financially—capitalized as project setup costs or absorbed as operational expenses?
2. During the transition window when both warehouses operate simultaneously, what is the policy for splitting shared regional overhead between them?

---

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.
