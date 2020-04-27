from datetime import datetime


class WeatherRecordDto:
	def __init__(self, weather_station_id: int, timestamp: datetime, temperature: float, humidity: float, air_pressure: float):
		self.weather_station_id = weather_station_id
		self.timestamp = timestamp
		self.temperature = temperature
		self.humidity = humidity
		self.air_pressure = air_pressure
