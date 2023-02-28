import 'source-map-support/register';
import { ContextKeys } from '@guardian/cdk/lib/constants';
import { App } from 'aws-cdk-lib';
import { GeoipDbRefresher } from '../lib/geoip-db-refresher';

const app = new App({
	context: {
		[ContextKeys.REPOSITORY_URL]: 'guardian/ophan-geoip-db-refresher',
	},
});
new GeoipDbRefresher(app, 'GeoipDbRefresher-PROD', {
	stack: 'ophan',
	stage: 'PROD',
});
