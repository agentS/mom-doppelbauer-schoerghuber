import os
from os.path import join, dirname
from dotenv import load_dotenv
from datetime import datetime

from jinja2 import Environment, FileSystemLoader
from tornado.web import RequestHandler
from functools import partial
from threading import Thread
from bokeh.embed import server_document
from bokeh.server.server import Server

from bokeh.models import ColumnDataSource
from bokeh.plotting import figure, curdoc

from tornado import gen

#from proton.reactor import Container
from amqp.proton_tornado import Container
from amqp.WeatherRecordReceiver import WeatherRecordReceiver
from dto.WeatherRecordDto import WeatherRecordDto


dotenv_path = join(dirname(__file__), '.env')
load_dotenv(dotenv_path)

environment = Environment(loader=FileSystemLoader('templates'))

def dashboard_application(document):
	data_source = ColumnDataSource({
		'timestamps': [],
		'temperature_values': [],
		'humidity_values': [],
		'air_pressure_values': [],
	})

	plot = figure(
		title = "Dashboard for weather station #" + os.getenv('WEATHER_STATION_ID'),
		plot_width = 1280,
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

	document.add_root(plot)
	document.title = "weather station dashboard"

#document = curdoc()

def handle_weather_record(dto: WeatherRecordDto):
	print(dto)
	#document.add_next_tick_callback(partial(update_data, dto = dto))

server = Server({'/': dashboard_application})
server.start()

amqp_receiver = WeatherRecordReceiver(
	url = os.getenv('AMQP_URL'),
	queue = os.getenv('AMQP_QUEUE'),
	username = os.getenv('AMQP_USERNAME'),
	password = os.getenv('AMQP_PASSWORD'),
	container_id = os.getenv('AMQP_CONTAINER_ID'),
	weather_station_id = int(os.getenv('WEATHER_STATION_ID')),
	record_received_callback = handle_weather_record
)
amqp_receiver.container = Container(amqp_receiver, loop = server.io_loop)
amqp_receiver.container.initialise()

if __name__ == '__main__':
	from bokeh.util.browser import view

	print('Opening Bokeh application on http://localhost:5006/')

	server.io_loop.add_callback(server.show, "/")
	server.io_loop.start()
