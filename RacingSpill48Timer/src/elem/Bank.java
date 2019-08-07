package elem;

public class Bank {
	private int points;
	private int money;
	private int[] inflation;

	public Bank() {
		//FIXME
		inflation = new int[8];
	}
	
	public boolean buyWithMoney(int amount, int i) {
		boolean res = false;
		
		if(amount <= money) {
			money -= amount;
			inflation[i]++;
			res = true;
		}
		
		return res;
	}
	
	public void addMoney(int amount) {
		money += amount;
	}
	
	public boolean buyWithPoints(int amount, int i) {
		boolean res = false;
		
		if(amount <= points) {
			points -= amount;
			inflation[i]++;
			res = true;
		}
		
		return res;
	}
	
	public boolean canAffordMoney(int cost) {
		return cost <= money;
	}
	
	public boolean canAffordPoints(int cost) {
		return cost <= points;
	}
	
	public void addPoints(int amount) {
		points += amount;
	}
	
	public int getPoints() {
		return points;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public int getMoney() {
		return money;
	}

	public void setMoney(int money) {
		this.money = money;
	}

	public int getInflation(int i ) {
		return inflation[i];
	}

}