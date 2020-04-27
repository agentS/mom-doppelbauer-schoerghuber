import machine
from dht import DHT22
from bme280_int import BME280 as BMP280
from umqtt.simple import MQTTClient

DHT22_SENSOR_PIN = 0
temperature_humidity_sensor = DHT22(machine.Pin(DHT22_SENSOR_PIN))

i2c_bus = machine.I2C(scl = machine.Pin(5), sda = machine.Pin(4))
air_pressure_sensor = BMP280(i2c = i2c_bus, address = 0x77)

def read_temperature_humidity_sensor_data():
	temperature_humidity_sensor.measure()
	return (
		temperature_humidity_sensor.temperature(),
		temperature_humidity_sensor.humidity()
	)

def read_air_pressure_sensor_data():
	temperature, air_pressure, _ = air_pressure_sensor.read_compensated_data()
	return (temperature / 100, air_pressure / (100 * 256))

def transmit_sensor_data(temperature, humidity, air_pressure, weather_station_id, server = "127.0.0.1"):
	client = MQTTClient('weather_station', server)
	client.connect()
	topic_name = 'sensor-data/' + str(weather_station_id)
	client.publish(str.encode(topic_name), b'{:.2f};{:.2f};{:.2f}'.format(temperature, humidity, air_pressure))
	client.disconnect()

WEATHER_STATION_ID = 10

if __name__ == '__main__':
	temperature, humidity = read_temperature_humidity_sensor_data()
	_, air_pressure = read_air_pressure_sensor_data()

	transmit_sensor_data(temperature, humidity, air_pressure, WEATHER_STATION_ID, "192.168.0.116")

	real_time_clock = machine.RTC()
	real_time_clock.irq(trigger=real_time_clock.ALARM0, wake=machine.DEEPSLEEP)
	#SLEEP_TIME = 5000
	SLEEP_TIME = 3600000
	real_time_clock.alarm(real_time_clock.ALARM0, SLEEP_TIME)
	machine.deepsleep()
