package audio_steganography;

public class DecodeData {
	private byte[] imgBytes;
	private String imgExtension;
	
	public DecodeData(byte[] imgBytes, String imgExtension) {
		super();
		this.imgBytes = imgBytes;
		this.imgExtension = imgExtension;
	}

	public byte[] getImgBytes() {
		return imgBytes;
	}

	public void setImgBytes(byte[] imgBytes) {
		this.imgBytes = imgBytes;
	}

	public String getImgExtension() {
		return imgExtension;
	}

	public void setImgExtension(String imgExtension) {
		this.imgExtension = imgExtension;
	}

}
