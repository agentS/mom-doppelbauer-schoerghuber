import React, { Component } from 'react';
import axios from 'axios'
import { Table, Button, Modal, ModalHeader, ModalBody, ModalFooter, Label, Input, FormGroup  } from 'reactstrap';

class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      stations: [],
      isLoaded: false,
      newStationModal: false,
      editStationModal: false,
      stationData: {
        id: -1,
        name: ''
      }
    }
  }

  componentDidMount() {
    this.findAllStations();
  }

  toggleNewStationModal(){
    this.setState({newStationModal: !this.state.newStationModal});
  }

  toggleEditStationModal(){
    this.setState({editStationModal: !this.state.editStationModal});
  }

  findAllStations(){
    axios.get('http://localhost:8080/api/stations')
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
    axios.post('http://localhost:8080/api/stations', this.state.stationData)
      .then(resp => {
        let { stations } = this.state;
        stations.push(resp.data);
        this.setState({ stations, newStationModal: false, stationData: {
          id:-1, name:''
        }});
      });
  }

  updateStation(){
    axios.put('http://localhost:8080/api/stations', this.state.stationData)
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

  render() {
    var { isLoaded, stations } = this.state;
    if(!isLoaded) {
      return <div>Loading stations ...</div>
    } else {
      return (
        <div className="App">
          <Button color="primary" onClick={this.toggleNewStationModal.bind(this)}>+ add new station</Button>
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
          <Table>
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
                      <Button color="success" size="sm" className="mr-2" onClick={this.editStation.bind(this, station.id, station.name)}>Edit</Button>
                      <Button color="success" size="sm">Subscribe</Button>
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
