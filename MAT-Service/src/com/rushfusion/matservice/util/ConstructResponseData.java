package com.rushfusion.matservice.util;

public class ConstructResponseData {

	public static final int STB_PORT = 6806;

	public static byte[] SearchResponse(int taskno, String stbname,String ip) {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>"
				+ "<Package> "
				+ "<Property  name = 'cmd' vaule = 'searchresp' /> "
				+ "<Property  name = 'taskno' vaule = '" + taskno + "' /> "
				+ "<Property  name = 'IP' vaule = '" + ip + "' /> "
				+ "<Property  name = 'username' vaule = '" + stbname + "' /> "
				+ "<Property  name = 'port' vaule = '" + STB_PORT + "' /> "
				+ "</Package>";

		byte[] data = stringToBytes(xml);
		return data;
	}

	public static byte[] SeekResp(String ip, int pos) {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>"
				+ "<Package> <Property  name = 'cmd' vaule = 'seekresp' />"
				+ " <Property  name = 'taskno' vaule = '" + 1 + "' /> "
				+ "<Property  name = 'IP' vaule = '" + ip + "' /> "
				+ "<Property  name = 'port' vaule = '" + STB_PORT + "' /> "
				+ "<Property  name = 'pos' vaule = '" + pos + "' /> "
				+ "</Package>";
		byte[] data = stringToBytes(xml);
		return data;
	}

	public static byte[] CompleteResp(String mIp) {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>"
				+ "<Package> "
				+ "<Property  name = 'cmd' vaule = 'completeresp' /> "
				+ "<Property  name = 'taskno' vaule = '123456' /> "
				+ "<Property  name = 'IP' vaule = '" + mIp + "' /> "
				+ "<Property  name = 'port' vaule = '" + STB_PORT + "' /> "
				+ "</Package>";

		byte[] data = stringToBytes(xml);
		return data;
	}

	public static byte[] ErrorResp(String mIp,int errorCode) {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>"
				+ "<Package> "
				+ "<Property  name = 'cmd' vaule = 'errorresp' /> "
				+ "<Property  name = 'taskno' vaule = '123456' /> "
				+ "<Property  name = 'IP' vaule = '" + mIp + "' /> "
				+ "<Property  name = 'port' vaule = '" + STB_PORT + "' /> "
				+ "<Property  name = 'errorCode' vaule = '" + errorCode + "' /> "
				+ "</Package>";

		byte[] data = stringToBytes(xml);
		return data;
	}

	private static byte[] stringToBytes(String xml) {
		byte[] xml_bytes = xml.getBytes();
		byte[] headlen_bytes = Tools.intToByteArray(12);
		byte[] bodylen_bytes = Tools.intToByteArray(xml_bytes.length);
		byte[] version_bytes = Tools.intToByteArray(1);
		byte[] data = new byte[12 + xml_bytes.length];
		System.arraycopy(headlen_bytes, 0, data, 0, 4);
		System.arraycopy(bodylen_bytes, 0, data, 4, 4);
		System.arraycopy(version_bytes, 0, data, 8, 4);
		System.arraycopy(xml_bytes, 0, data, 12, xml_bytes.length);
		return data;
	}

}
