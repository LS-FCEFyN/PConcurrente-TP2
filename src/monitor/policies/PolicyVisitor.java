package monitor.policies;

public interface PolicyVisitor {
    void visit(BalancedPolicy balancedPolicy);

    void visit(PriorityPolicy priorityPolicy);

    void visit(RandomPolicy randomPolicy);
}
