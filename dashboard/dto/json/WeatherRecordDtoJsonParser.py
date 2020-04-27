import json
from datetime import datetime

from dto.WeatherRecordDto import WeatherRecordDto


DATE_TIME_FORMAT = '%Y-%m-%dT%H:%M:%S'

def create_weather_record_dto_from_json(json_string: str):
	json_representation = json.loads(json_string)
	return WeatherRecordDto(
		weather_station_id = int(json_representation['weatherStationId']),
		timestamp = datetime.strptime(json_representation['timestamp'], DATE_TIME_FORMAT),
		temperature = float(json_representation['measurement']['temperature']),
		humidity = float(json_representation['measurement']['humidity']),
		air_pressure = float(json_representation['measurement']['airPressure']),
	)
