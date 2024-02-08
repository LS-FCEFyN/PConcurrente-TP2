package monitor.policies;

import java.util.Random;

public class RandomPolicy extends TransitionPolicy {

	private Random random_generator;

	public RandomPolicy() {
		random_generator = new Random(System.currentTimeMillis());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Integer which(Object enabled) {
		Integer index;
		Boolean[] booleanArray = (Boolean[]) enabled;
		int retries = booleanArray.length * 3;
		do {
			retries--;
			if (retries < 0) {
				return sequentialFindFirst(booleanArray);
			}
			index = random_generator.nextInt(booleanArray.length);
		} while (!booleanArray[index]);
		return index;
	}

	private Integer sequentialFindFirst(Boolean[] enabled) {
		for (int i = 0; i < enabled.length; i++) {
			if (enabled[i]) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public void accept(PolicyVisitor visitor) {
		visitor.visit(this);
	}
}
