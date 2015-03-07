package net.team6460.mcp23017;

import java.io.IOException;
import java.util.Arrays;

import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.wiringpi.GpioInterruptEvent;

public class MCP23017Device {

	protected InterruptHandler intr;
	/**
	 * Default IOCON state. Open-drain interrupts are used to allow any
	 * voltage supported by the MCP23017 to be used on VDD.
	 */
	private static final byte IOCON_BASE = 0b00000100;
	/**
	 * Mask for mirroring the interrupts.
	 */
	private static final byte IOCON_MIRROR = 0b01000000;

	// If initial bank is incorrect, then use 0x05
	private static final byte IOCON_ADDR = 0x0a;

	private static final byte A_TO_B_OFFSET = 0x01;

	private static final byte IODIR_ADDR = 0x00;

	private static final byte IPOL_ADDR = 0x02;

	private static final byte GPINTEN_ADDR = 0x04;

	private static final byte DEFVAL_ADDR = 0x06;

	private static final byte INTCON_ADDR = 0x08;

	private static final byte GPPU_ADDR = 0x0c;

	private static final byte INTF_ADDR = 0x0e;

	private static final byte INTCAPA_ADDR = 0x10;

	private static final byte GPIO_ADDR = 0x12;

	private static final byte OLAT_ADDR = 0x14;

	private static final byte ZEROS = 0b00000000;

	public enum MCPPinMode {
		MODE_OUTPUT, MODE_INPUT, MODE_INPUT_PULLUP
	}

	// by default all outputs
	private MCPPinMode[] pinModes = new MCPPinMode[16];

	public synchronized void init() throws IOException {
		Arrays.fill(pinModes, MCPPinMode.MODE_INPUT);
		byte iocon = (byte) (IOCON_BASE | (intr.shouldMirrorInterrupts() ? IOCON_MIRROR : 0));
		dev.write(IOCON_ADDR, iocon);
		dev.write(INTCON_ADDR, ZEROS);
		dev.write(INTCON_ADDR + A_TO_B_OFFSET, ZEROS);
		dev.write(IPOL_ADDR, ZEROS);
		dev.write(IPOL_ADDR + A_TO_B_OFFSET, ZEROS);
		intr.startInterruptHandler();

	}

	protected synchronized void enableInterrupt(int pin) throws IOException {
		if (pin < 0 || pin > 15)
			throw new IllegalArgumentException("Invalid pin number");
		if (pinModes[pin] == MCPPinMode.MODE_OUTPUT)
			throw new IllegalArgumentException("Pin is currently an output.");
		int pinBit = pin % 8;
		int addrGpinten = GPINTEN_ADDR + (pin > 7 ? A_TO_B_OFFSET : 0);
		byte newGpinten = (byte) (dev.read(addrGpinten) | 1 << pinBit);
		dev.write(addrGpinten, newGpinten);
	}

	public synchronized void setPinMode(int pin, MCPPinMode mode) throws IOException {
		if (pin < 0 || pin > 15)
			throw new IllegalArgumentException("Invalid pin number");
		pinModes[pin] = mode;
		int pinBit = pin % 8;
		int addrIodir = IODIR_ADDR + (pin > 7 ? A_TO_B_OFFSET : 0);
		if (mode == MCPPinMode.MODE_OUTPUT)
			dev.write(addrIodir, (byte) (dev.read(addrIodir) & (~(1 << pinBit))));
		else {
			dev.write(addrIodir, (byte) (dev.read(addrIodir) | (1 << pinBit)));

			int addrGppu = GPPU_ADDR + (pin > 7 ? A_TO_B_OFFSET : 0);
			if (mode == MCPPinMode.MODE_INPUT_PULLUP)
				dev.write(addrIodir, (byte) (dev.read(addrGppu) | (1 << pinBit)));

			else {
				dev.write(addrIodir, (byte) (dev.read(addrGppu) & (~(1 << pinBit))));

			}
		}

	}

	public enum MCPPinState {
		STATE_LOW, STATE_HIGH
	}

	public synchronized MCPPinState readPin(int pin) throws IOException {
		if (pin < 0 || pin > 15)
			throw new IllegalArgumentException("Invalid pin number");
		if (pinModes[pin] == MCPPinMode.MODE_OUTPUT) {
			throw new IllegalArgumentException("Pin is currently an output.");
		}
		;
		int pinBit = pin % 8;
		int addrGpio = GPIO_ADDR + (pin > 7 ? A_TO_B_OFFSET : 0);
		byte gpioVal = (byte) dev.read(addrGpio);
		return ((gpioVal & (1 << pinBit)) != 0) ? MCPPinState.STATE_LOW : MCPPinState.STATE_HIGH;
	}

	public class SeparateInterruptHandler implements InterruptHandler {
		private final GpioPinDigitalInput pinA, pinB;

		public SeparateInterruptHandler(Pin pinA, Pin pinB) {
			super();
			this.pinA = Singletons.gpio.provisionDigitalInputPin(pinA, PinPullResistance.PULL_UP);
			this.pinB = Singletons.gpio.provisionDigitalInputPin(pinB, PinPullResistance.PULL_UP);

		}

		@Override
		public boolean shouldMirrorInterrupts() {
			return false;

		}

		protected void dispatchPinAInterrupts() {
			// TODO Auto-generated method stub

		}

		protected void dispatchPinBInterrupts() {
			// TODO Auto-generated method stub

		}

		@Override
		public void startInterruptHandler() {
			pinA.addListener(new GpioPinListenerDigital() {
				@Override
				public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
					if (event.getState() == PinState.LOW)
						SeparateInterruptHandler.this.dispatchPinAInterrupts();
				}

			});

			pinB.addListener(new GpioPinListenerDigital() {
				@Override
				public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
					if (event.getState() == PinState.LOW)
						SeparateInterruptHandler.this.dispatchPinBInterrupts();
				}

			});
		}

	}

	public class JointInterruptHandler implements InterruptHandler {
		private final GpioPinDigitalInput intrPin;

		public JointInterruptHandler(Pin pin) {
			intrPin = Singletons.gpio.provisionDigitalInputPin(pin, PinPullResistance.PULL_UP);
		}

		@Override
		public boolean shouldMirrorInterrupts() {
			return true;
		}

		@Override
		public void startInterruptHandler() {
			intrPin.addListener(new GpioPinListenerDigital() {
				@Override
				public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
					JointInterruptHandler.this.dispatchInterrupts();
				}

			});
		}

		protected void dispatchInterrupts() {
			// TODO Auto-generated method stub

		}

	}

	public class NullInterruptHandler implements InterruptHandler {

		@Override
		public boolean shouldMirrorInterrupts() {
			return false;
		}

		@Override
		public void startInterruptHandler() {
			// noop

		}

	}

	private I2CDevice dev;

	public MCP23017Device(I2CDevice dev) {
		super();
		this.dev = dev;
	}

	protected MCP23017Device() {
		// private no-op constructor
	}

	public interface InterruptHandler {
		boolean shouldMirrorInterrupts();

		void startInterruptHandler();
	}

	public static class Builder {
		private Pin interruptBankA, interruptBankB;
		private int busId = -1; // initially invalid
		private byte addr = -1; // initially invalid

		public MCP23017Device build() throws IllegalStateException, IOException {
			if (this.busId < 0)
				throw new IllegalStateException("Bus ID not set.");
			if (this.addr < 0x20 || this.addr > 0x27)
				throw new IllegalStateException("Address must be between 0x20 and 0x27, inclusive");
			I2CBus bus = Singletons.getI2CBus(busId);
			if (bus == null)
				throw new IOException("Failed to open the I2C bus.");
			MCP23017Device dev = new MCP23017Device(bus.getDevice(addr));

			if (interruptBankA == null && interruptBankB == null) {
				dev.intr = dev.new NullInterruptHandler();
			} else if (interruptBankA == interruptBankB) {
				// doesn't matter which bank's pin to pass since
				// they have the
				// same interrupt pin
				dev.intr = dev.new JointInterruptHandler(interruptBankA);
			} else {
				dev.intr = dev.new SeparateInterruptHandler(interruptBankA, interruptBankB);
			}
			dev.init();
			return dev;
		}

		public Pin getInterruptBankA() {
			return interruptBankA;
		}

		public Builder setInterruptPins(Pin interruptBankA, Pin interruptBankB) {
			if (interruptBankA == null || interruptBankB == null) {
				throw new IllegalArgumentException("Interrupt pins may not be null");
			}
			this.interruptBankA = interruptBankA;
			this.interruptBankB = interruptBankB;
			return this;
		}

		public Pin getInterruptBankB() {
			return interruptBankB;
		}

		public Builder setSingleInterruptPin(Pin pin) {
			return this.setInterruptPins(pin, pin);
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
