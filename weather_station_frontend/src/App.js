import React, { Component } from 'react';
import axios from 'axios'
import { Breadcrumb, BreadcrumbItem, Table, Button, Modal, ModalHeader, ModalBody, ModalFooter, Label, Input, FormGroup  } from 'reactstrap';

const API_ENDPOINT_PREFIX = "http://localhost:8080/api";
const SOCKET_ENDPOINT_PREFIX = "ws://localhost:8080/stations/socket/";
var connected = false;
var socket;

class App extends Component {

  constructor(props) {
    super(props);
    this.state = {
      stations: [],
      isLoaded: false,
      newStationModal: false,
      editStationModal: false,
      selectedStation: -1,
      stationEvent: {
        weatherStationId: 1,
        measurement: {
          temperature: null,
          humidity: null,
          airPressure: null
        }
      },
      stationData: {
        id: -1,
        name: ''
      }
    }
  }

  componentDidMount() {
    this.findAllStations();
    // this.subscribeForStationData(this);
  }

  /*subscribeForStationData(ob){
    var source = new EventSource(API_ENDPOINT_PREFIX + "/stations/stream");
    source.onmessage = function (event) {
      console.log(event.data);
      let message = JSON.parse(event.data);
      if(ob.state.selectedStation === message.weatherStationId){
        ob.setState({ stationEvent: message });
        console.log("set state");
      }
    };
  }*/

  connectToStationSocket(stationId, ob) {
    if(connected){
        socket.close();
        connected = false;
    }
    if (! connected) {
        console.log("stationId: " + stationId);
        socket = new WebSocket(SOCKET_ENDPOINT_PREFIX + stationId);
        socket.onopen = function() {
            connected = true;
            console.log("Connected to the web socket");
        };
        socket.onmessage =function(m) {
            console.log("Got message: " + m.data);
            let message = JSON.parse(m.data);
            ob.setState({ stationEvent: message });
        };
    }
};

  toggleNewStationModal(){
    this.setState({newStationModal: !this.state.newStationModal});
  }

  toggleEditStationModal(){
    this.setState({editStationModal: !this.state.editStationModal});
  }

  findAllStations(){
    axios.get(API_ENDPOINT_PREFIX + '/stations')
      .then(res => {
        let tmpStations = res.data;
        tmpStations.sort(function(a, b) {
          return (a.id - b.id);
        });
        this.setState({
          isLoaded: true,
          stations: tmpStations
        })
      });
  }

  addStation(){
    axios.post(API_ENDPOINT_PREFIX + '/stations', this.state.stationData)
      .then(resp => {
        let { stations } = this.state;
        stations.push(resp.data);
        this.setState({ stations, newStationModal: false, stationData: {
          id:-1, name:''
        }});
      });
  }

  updateStation(){
    axios.put(API_ENDPOINT_PREFIX + '/stations', this.state.stationData)
      .then(resp => {
        this.findAllStations(); // reload stations
        this.setState({ editStationModal: false, stationData: {
          id:-1, name:''
        }});
      });
  }

  editStation(id, name){
    this.setState({
      stationData: {
        id: id,
        name: name
      }
    });
    this.toggleEditStationModal();
  }

  changeSelectedStation(id){
    this.setState({ 
      selectedStation: id,
      stationEvent: {
        weatherStationId: null,
        measurement: {
          temperature: null,
          humidity: null,
          airPressure: null
        }
      }});
    this.connectToStationSocket(id, this);
  }

  render() {
    var { isLoaded, stations } = this.state;
    if(!isLoaded) {
      return <div>Loading stations ...</div>
    } else {
      return (
        <div className="App">
          <Breadcrumb>
            <BreadcrumbItem active>
              ({this.state.selectedStation}) {this.state.stationEvent.weatherStationId}: T{this.state.stationEvent.measurement.temperature} H{this.state.stationEvent.measurement.humidity} P{this.state.stationEvent.measurement.airPressure} 
            </BreadcrumbItem>
          </Breadcrumb>
          <Button color="secondary" onClick={this.toggleNewStationModal.bind(this)}>+ add new station</Button>
          <Modal isOpen={this.state.newStationModal} toggle={this.toggleNewStationModal.bind(this)}>
            <ModalHeader toggle={this.toggleNewStationModal.bind(this)}>Add a new weather station</ModalHeader>
            <ModalBody>
              <FormGroup>
                <Label for="name">Name</Label>
                <Input id="name" value={this.state.stationData.name} onChange={(e) => {
                  let { stationData } = this.state;
                  stationData.name = e.target.value;
                  this.setState({stationData});
                }} />
              </FormGroup>
            </ModalBody>
            <ModalFooter>
              <Button color="primary" onClick={this.addStation.bind(this)}>Add Station</Button>
              <Button color="secondary" onClick={this.toggleNewStationModal.bind(this)}>Cancel</Button>
            </ModalFooter>
          </Modal>
          <Modal isOpen={this.state.editStationModal} toggle={this.toggleEditStationModal.bind(this)}>
            <ModalHeader toggle={this.toggleEditStationModal.bind(this)}>Update weather station</ModalHeader>
            <ModalBody>
              <FormGroup>
                <Label for="name">Name</Label>
                <Input id="name" value={this.state.stationData.name} onChange={(e) => {
                  let { stationData } = this.state;
                  stationData.name = e.target.value;
                  this.setState({stationData});
                }} />
              </FormGroup>
            </ModalBody>
            <ModalFooter>
              <Button color="primary" onClick={this.updateStation.bind(this)}>Update Station</Button>
              <Button color="secondary" onClick={this.toggleEditStationModal.bind(this)}>Cancel</Button>
            </ModalFooter>
          </Modal>
          <Table hover>
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {
                stations.map(station => (
                  <tr key={station.id}>
                    <td>{station.id}</td>
                    <td>{station.name}</td>
                    <td>
                      <Button color="secondary" size="sm" className="mr-2" 
                        onClick={this.editStation.bind(this, station.id, station.name)}>Edit</Button>
                      <Button color="secondary" size="sm" 
                        onClick={this.changeSelectedStation.bind(this, station.id)}>Select</Button>
                    </td>
                  </tr>
                ))
              };
            </tbody>
          </Table>
        </div>
      );
    }
  }
}

export default App;
