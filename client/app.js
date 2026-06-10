const { createApp } = Vue;

const SERVER_URL = "ws://localhost:8080";

createApp({
    data() {
        return {
            socket: null,
            status: "no connection",
            loggedIn: false,
            username: "",
            password: "",
            draft: "",
            messages: []
        };
    },
    mounted() {
        this.socket = new WebSocket(SERVER_URL);

        this.socket.onopen = () => { this.status = "connected"; };
        this.socket.onclose = () => { this.status = "the connection is closed"; };
        this.socket.onmessage = (event) => { this.onPacket(JSON.parse(event.data)); };
    },
    methods: {
        send(type, data) {
            this.socket.send(JSON.stringify({ type, data }));
        },

        login() {
            this.send("LOGIN", { username: this.username, password: this.password });
        },

        register() {
            this.send("REGISTER", { username: this.username, password: this.password });
        },

        sendChat() {
            if (this.draft.trim() === "") return;
            this.send("CHAT_MSG", { text: this.draft });
            this.draft = "";
        },

        onPacket(packet) {
            switch (packet.type) {
                case "AUTH_SUCCESS":
                    this.loggedIn = true;
                    this.status = "looged " + this.username;
                    break;
                case "AUTH_FAIL":
                    this.status = "incorrect login or password";
                    break;
                case "CHAT_MSG":
                    this.messages.push(packet.data.text);
                    break;
                case "ERROR":
                    this.status = "error: " + packet.data.message;
                    break;
            }
        }
    }
}).mount("#app");
