metadata {
	// Automatically generated. Make future change here.
	definition (name: "HCO", namespace: "smartthings", author: "SmartThings") {
		capability "Actuator"
		capability "Switch"
		capability "Sensor"
		capability "Refresh"
	}

	// simulator metadata
	simulator {
		// status messages
		status "on": "on/off: 1"
		status "off": "on/off: 0"

		// reply messages
		reply "zcl on-off on": "on/off: 1"
		reply "zcl on-off off": "on/off: 0"
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: 'On', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState: "turningOff"
				attributeState "off", label: 'Off', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
				attributeState "turningOn", label: 'Turning On', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState: "turningOff"
				attributeState "turningOff", label: 'Turning Off', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
			
			}
		}
		main "switch"
		details(["switch","refresh"])
	}
}


// Parse incoming device messages to generate events
def parse(String description) {
	log.debug "description is $description"

	def finalResult = zigbee.getKnownDescription(description)
	def event = [:]

	//TODO: Remove this after getKnownDescription can parse it automatically
	if (!finalResult && description!="updated")
		finalResult = getPowerDescription(zigbee.parseDescriptionAsMap(description))

	if (finalResult) {
		log.info "final result = $finalResult"
		if (finalResult.type == "update") {
			log.info "$device updates: ${finalResult.value}"
			event = null
		}
		else if (finalResult.type == "power") {
			def powerValue = (finalResult.value as Integer)/10
			event = createEvent(name: "power", value: powerValue, descriptionText: '{{ device.displayName }} power is {{ value }} Watts', translatable: true)
			/*
				Dividing by 10 as the Divisor is 10000 and unit is kW for the device. AttrId: 0302 and 0300. Simplifying to 10
				power level is an integer. The exact power level with correct units needs to be handled in the device type
				to account for the different Divisor value (AttrId: 0302) and POWER Unit (AttrId: 0300). CLUSTER for simple metering is 0702
			*/
		}
		else {
			def descriptionText = finalResult.value == "on" ? '{{ device.displayName }} is On' : '{{ device.displayName }} is Off'
			event = createEvent(name: finalResult.type, value: finalResult.value, descriptionText: descriptionText, translatable: true)
		}
	}
	else {
		def cluster = zigbee.parse(description)

		if (cluster && cluster.clusterId == 0x0006 && cluster.command == 0x07){
			if (cluster.data[0] == 0x00) {
				log.debug "ON/OFF REPORTING CONFIG RESPONSE: " + cluster
				event = createEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
			}
			else {
				log.warn "ON/OFF REPORTING CONFIG FAILED- error code:${cluster.data[0]}"
				event = null
			}
		}
		else {
			log.warn "DID NOT PARSE MESSAGE for description : $description"
			log.debug "${cluster}"
		}
	}
	return event
}

// Commands to device
def on() {
	[
			'zcl on-off on',
			'delay 200',
			"send 0x${zigbee.deviceNetworkId} 0x01 0x${zigbee.endpointId}",
			'delay 2000'

	]

}

def off() {
	[
			'zcl on-off off',
			'delay 200',
			"send 0x${zigbee.deviceNetworkId} 0x01 0x${zigbee.endpointId}",
			'delay 2000'
	]
}


/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	return zigbee.onOffRefresh()
}

def refresh() {
	zigbee.onOffRefresh()
}
