package elem;

public class ColorBytes {

	private byte r, g, b, a;

	public ColorBytes(int r, int g, int b, int a) {
		this.r = (byte) r;
		this.g = (byte) g;
		this.b = (byte) b;
		this.a = (byte) a;
	}

	public byte r() {
		return r;
	}

	public void setR(byte r) {
		this.r = r;
	}

	public byte g() {
		return g;
	}

	public void setG(byte g) {
		this.g = g;
	}

	public byte b() {
		return b;
	}

	public void setB(byte b) {
		this.b = b;
	}

	public byte a() {
		return a;
	}

	public void setA(byte a) {
		this.a = a;
	}

}
