import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App.jsx";
import "@fontsource-variable/noto-sans-sc";
import "@fontsource-variable/noto-sans-mono";
import "@fontsource-variable/noto-serif-sc";
import "./styles/tokens.css";
import "./styles/base.css";
import "./styles/public.css";
import "./styles/app-shell.css";
import "./styles/workflow.css";
import "./styles/academic-workspace.css";
import "./styles/onboarding.css";

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
