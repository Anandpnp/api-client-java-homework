	@FindBy(css="")
	private WebElement webElement;
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2000-2024
//    by aixigo AG, Aachen, Germany.
//
//  All rights reserved. This material is confidential and proprietary to AIXIGO AG and no part of this
//  material should be reproduced, published in any form by any means, electronic or mechanical including
//  photocopy or any information storage or retrieval system nor should the material be disclosed to third
//  parties without the express written authorization of AIXIGO AG.
//
//  aixigo AG
//  https://www.aixigo.com
//  Aachen, Germany
//

import process from 'node:process';
import createClient from 'openapi-fetch';
import type { paths } from './schema.js';
import { Client } from './types.js';

async function main(args: string[]) {
  const baseUrl = args.length > 0 ? args[0] : 'https://petstore.swagger.io/v2';

  const client: Client = createClient<paths>({
    baseUrl,
    headers: {
      'X-ID-Token': process.env.X_ID_TOKEN,
      Accept: 'application/json'
    }
  });

  const { error } = await client.POST('/pet', {
    body: {
      id: 124,
      name: 'Fred',
      status: 'available',
      photoUrls: [],
      tags: []
    }
  });
  if (error) {
    console.error(error);
  }

  const { data } = await client.GET('/pet/{petId}', {
    params: {
      path: { petId: 124 }
    }
  });
  console.log(data);
}

main(process.argv.slice(2)).then(() => {
  process.exitCode = 0;
});
