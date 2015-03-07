package net.team6460.mcp23017;

import com.pi4j.io.gpio.RaspiPin;

public class MCP23017Device {
	protected MCP23017Device() {
		// private no-op constructor
	}

	public static class Builder {
		private RaspiPin interruptBankA, interruptBankB;
		private int busId = -1; // initially invalid
		private byte addr = -1; // initially invalid

		public MCP23017Device build() throws IllegalStateException {
			if(this.busId < 0) throw new IllegalStateException("Bus ID not set.");
			if(this.addr < 0x20 || this.addr > 0x27) throw new IllegalStateException("Address must be between 0x20 and 0x27, inclusive");
			
			if(interruptBankA == null && interruptBankB == null){
				
			}
			
			return new MCP23017Device();
		}
		
		public RaspiPin getInterruptBankA() {
			return interruptBankA;
		}

		public Builder setInterruptBankA(RaspiPin interruptBankA) {
			this.interruptBankA = interruptBankA;
			return this;
		}

		public RaspiPin getInterruptBankB() {
			return interruptBankB;
		}

		public Builder setInterruptBankB(RaspiPin interruptBankB) {
			this.interruptBankB = interruptBankB;
			return this;
		}

		public int getBusId() {
			return busId;
		}

		public Builder setBusId(int busId) {
			this.busId = busId;
			return this;
		}

		public byte getAddr() {
			return addr;
		}

		public Builder setAddr(byte addr) {
			this.addr = addr;
			return this;
		}

		public Builder setAddr(boolean a2, boolean a1, boolean a0) {
			this.addr = (byte) (32 + (a2 ? 4 : 0) + (a1 ? 2 : 0) + (a0 ? 1 : 0));
			return this;
		}

	}
}
