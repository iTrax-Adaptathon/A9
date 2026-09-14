function saveAppointment(event) {
    event.preventDefault();

    const title = document.getElementById("title").value;
    const date = document.getElementById("date").value;
    const time = document.getElementById("time").value;
    const members = document.getElementById("members").value;

    const appointment = {
        title: title,
        date: date,
        time: time,
        members: members
    };

    localStorage.setItem(
        "syncslotAppointment",
        JSON.stringify(appointment)
    );

    displayAppointment();

    alert("Appointment Created Successfully!");

    document.querySelector("form").reset();
}

function displayAppointment() {
    const savedAppointment = localStorage.getItem("syncslotAppointment");

    const appointmentList = document.getElementById("appointment-list");

    if (!savedAppointment || !appointmentList) {
        return;
    }

    const appointment = JSON.parse(savedAppointment);

    appointmentList.className = "appointment-item";

    appointmentList.innerHTML = `
        <h3>${appointment.title}</h3>
        <p><strong>Date:</strong> ${appointment.date}</p>
        <p><strong>Time:</strong> ${appointment.time}</p>
        <p><strong>Members:</strong> ${appointment.members}</p>
    `;
}

displayAppointment();