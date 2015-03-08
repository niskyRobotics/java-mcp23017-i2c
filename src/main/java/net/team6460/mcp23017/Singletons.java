package net.team6460.mcp23017;

import java.io.IOException;
import java.util.HashMap;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;

public class Singletons {
	static final GpioController gpio = GpioFactory.getInstance();
	public static final GpioController getGPIO(){
		return gpio;
	}
	
	private static final HashMap<Integer, I2CBus> i2c = new HashMap<>();

	static synchronized final I2CBus getI2CBus(int id) throws IOException {
		return i2c.computeIfAbsent(id, (id_) -> {
			try {
				return I2CFactory.getInstance(id_);
			} catch (IOException e) {
				System.err.println("[MCP23017] IOException creating I2C bus: "+e.getMessage());
				return null;
			}
		});
	}
}
