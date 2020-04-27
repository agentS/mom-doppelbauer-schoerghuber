import os
from os.path import join, dirname
from dotenv import load_dotenv
from datetime import datetime

from functools import partial
from threading import Thread

from bokeh.models import ColumnDataSource
from bokeh.plotting import figure, curdoc

from tornado import gen

from proton.reactor import Container
from amqp.WeatherRecordReceiver import WeatherRecordReceiver
from dto.WeatherRecordDto import WeatherRecordDto


dotenv_path = join(dirname(__file__), '.env')
load_dotenv(dotenv_path)

data_source = ColumnDataSource({
	'timestamps': [],
	'temperature_values': [],
	'humidity_values': [],
	'air_pressure_values': [],
})

plot = figure(
	title = "Dashboard for weather station #" + os.getenv('WEATHER_STATION_ID'),
	plot_height = 500,
	tools="xpan,xwheel_zoom,xbox_zoom,reset",
	x_axis_type = 'datetime'
)
plot.x_range.follow = "end"

plot.circle(x = 'timestamps', y = 'temperature_values', color = 'red', source = data_source)
plot.line(x = 'timestamps', y = 'temperature_values', color = 'red', source = data_source)

plot.circle(x = 'timestamps', y = 'humidity_values', color = 'blue', source = data_source)
plot.line(x = 'timestamps', y = 'humidity_values', color = 'blue', source = data_source)

plot.circle(x = 'timestamps', y = 'air_pressure_values', color = 'purple', source = data_source)
plot.line(x = 'timestamps', y = 'air_pressure_values', color = 'purple', source = data_source)

@gen.coroutine
def update_data(dto: WeatherRecordDto):
	data_source.stream({
		'timestamps': [dto.timestamp],
		'temperature_values': [dto.temperature],
		'humidity_values': [dto.humidity],
		'air_pressure_values': [dto.air_pressure],
	})

document = curdoc()

def handle_weather_record(dto: WeatherRecordDto):
	document.add_next_tick_callback(partial(update_data, dto = dto))

def run_amqp_receiver():
	print('starting the AMQP receiver')
	amqp_receiver = Container(
		WeatherRecordReceiver(
			url = os.getenv('AMQP_URL'),
			queue = os.getenv('AMQP_QUEUE'),
			username = os.getenv('AMQP_USERNAME'),
			password = os.getenv('AMQP_PASSWORD'),
			container_id = os.getenv('AMQP_CONTAINER_ID'),
			weather_station_id = int(os.getenv('WEATHER_STATION_ID')),
			record_received_callback = handle_weather_record
		)
	)
	try:
		amqp_receiver.run()
	except KeyboardInterrupt:
		print('stopping the AMQP receiver')
	print('stopped the AMQP receiver')

document.add_root(plot)
document.title = "weather station dashboard"

amqp_receiver_thread = Thread(target = run_amqp_receiver)
amqp_receiver_thread.start()
