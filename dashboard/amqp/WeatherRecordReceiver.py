from __future__ import print_function
from proton.reactor import AtMostOnce, DurableSubscription, DynamicNodeProperties
from proton.handlers import MessagingHandler

from dto.WeatherRecordDto import WeatherRecordDto
from dto.json.WeatherRecordDtoJsonParser import create_weather_record_dto_from_json


class WeatherRecordReceiver(MessagingHandler):
	def __init__(self, url: str, queue: str, username: str, password: str, container_id: str, weather_station_id: int, record_received_callback):
		super(WeatherRecordReceiver, self).__init__()
		self.url = url
		self.queue = queue
		self.username = username
		self.password = password
		self.container_id = container_id
		self.weather_station_id = weather_station_id
		self.record_received_callback = record_received_callback
		self.received = 0

	def on_start(self, event):
		event.container.container_id = self.container_id
		self.connection = event.container.connect(
			self.url,
			address = self.queue,
			user = self.username,
			password = self.password,
		)
		event.container.create_receiver(
			self.connection, self.queue,
			name = self.queue,
			options = [AtMostOnce(), DurableSubscription()],
		)

	def on_message(self, event):
		weather_record_dto = create_weather_record_dto_from_json(event.message.body.decode("utf-8"))
		self.received += 1
		if weather_record_dto.weather_station_id == self.weather_station_id:
			self.record_received_callback(weather_record_dto)
