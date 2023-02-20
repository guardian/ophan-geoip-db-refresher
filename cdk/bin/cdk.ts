import "source-map-support/register";
import { App } from "aws-cdk-lib";
import { GeoipDbRefresher } from "../lib/geoip-db-refresher";

const app = new App();
new GeoipDbRefresher(app, "GeoipDbRefresher-PROD", { stack: "ophan", stage: "PROD" });
