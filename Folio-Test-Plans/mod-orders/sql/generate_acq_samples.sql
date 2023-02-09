CREATE OR REPLACE FUNCTION public.generate_vendors(organizations_amount integer,
                                                                   orders_per_vendor integer,
                                                                   polines_per_order integer,
                                                                   tenant text) RETURNS VOID as
$$

DECLARE
  -- set!!!
  orgName   text DEFAULT 'perf_test_vendor';
  orgCode   TEXT default 'PERF_TEST_ORG';
  vendor_id TEXT default 'e0fb5df2-cdf1-11e8-a8d5-f2801f1b9fd1'; -- AMAZ default vendor
  user_id   TEXT default '28d1057c-d137-11e8-a8d5-f2801f1b9fd1'; -- diku default user
BEGIN
  for org_counter in 1..organizations_amount
    loop
    -- Uncomment if needed to create new vendors
/*                  INSERT INTO diku_mod_organizations_storage.organizations (id, jsonb)
                    VALUES (public.uuid_generate_v5(public.uuid_nil(), 'organizations_uuid'),
                          jsonb_build_object('code', concat(orgCode, org_counter),
                                             'erpCode', '12345',
                                             'isVendor', true,
                                             'name', concat(orgName, org_counter),
                                             'status', 'Active',
                                             'metadata', jsonb_build_object(
                                                     'createdDate', '2023-02-08T00:00:00.000+0000',
                                                     'createdByUserId', user_id,
                                                     'updatedDate', '2023-02-08T00:00:00.000+0000',
                                                     'updatedByUserId', user_id
                                                 )
                              ))
                  RETURNING id INTO vendor_id;*/

      PERFORM public.generate_orders(orders_per_vendor, polines_per_order, vendor_id, tenant);
    end loop;
END
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION public.generate_orders(orders_per_vendor integer, polines_per_order integer, vendor_id text, tenant text) RETURNS VOID as
$$
DECLARE
  order_id    text;
  newPoNumber integer;
  user_id   TEXT default '28d1057c-d137-11e8-a8d5-f2801f1b9fd1'; -- diku default user

BEGIN
  for order_counter in 1..orders_per_vendor
    loop
      SELECT nextval('diku_mod_orders_storage.po_number') INTO newPoNumber;
      --
      INSERT INTO diku_mod_orders_storage.purchase_order (id, jsonb)
      VALUES (public.uuid_generate_v5(public.uuid_nil(), 'purchase_order_uuid'),
              jsonb_build_object('reEncumber', true,
                                 'workflowStatus', 'Pending',
                                 'poNumber', newPoNumber,
                                 'vendor', vendor_id,
                                 'orderType', 'One-Time',
                                 'metadata', jsonb_build_object(
                                   'createdDate', '2023-02-08T00:00:00.000+0000',
                                   'createdByUserId', user_id,
                                   'updatedDate', '2023-02-08T00:00:00.000+0000',
                                   'updatedByUserId', user_id
                                   )
                ))


      RETURNING id INTO order_id;
      PERFORM public.generate_polines(order_id, polines_per_order, newPoNumber, vendor_id);
    end loop;
END
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION public.generate_polines(order_id text, polines_per_order integer, ponumber integer, vendor_id text) RETURNS VOID as
$$
DECLARE
  polineNumber text;
  fundCode     text DEFAULT 'AFRICAHIST';
  fundId       text DEFAULT '7fbd5d84-62d1-44c6-9c45-6cb173998bbd'; -- AFRICAHIST
  locationId   text DEFAULT 'fcd64ce1-6995-48f0-840e-89ffa2288371'; -- fcd64ce1-6995-48f0-840e-89ffa2288371 "Main Library" KU/CC/DI/M
BEGIN
  for line_counter in 1..polines_per_order
    loop
      INSERT INTO diku_mod_orders_storage.po_line (id, jsonb)
      --                SELECT public.uuid_generate_v5(public.uuid_nil(), concat('BER2', _rollover_record->>'id', tr.id, fund.id)), jsonb_build_object

      VALUES (public.uuid_generate_v5(public.uuid_nil(), 'poline_uuid'),
              jsonb_build_object('acquisitionMethod', 'df26d81b-9d63-4ff8-bf41-49bf75cfa70e',
                                 'rush', false,
                                 'cost', json_build_object(
                                   'currency', 'USD',
                                   'discountType', 'percentage',
                                   'listUnitPrice', 1,
                                   'quantityPhysical', 1,
                                   'poLineEstimatedPrice', 1
                                   ),
                                 'fundDistribution', json_build_array(
                                   jsonb_build_object(
                                     'code', fundCode,
                                     'fundId', fundId,
                                     'distributionType', 'percentage',
                                     'value', 100
                                     )
                                   ),
                                 'locations', json_build_array(
                                   jsonb_build_object(
                                     'locationId', locationId,
                                     'quantity', 2,
                                     'quantityElectronic', 0,
                                     'quantityPhysical', 2)
                                   ),
                                 'alerts', json_build_array(),
                                 'source', 'User',
                                 'physical', jsonb_build_object(
                                   'createInventory', 'Instance, Holding, Item',
                                   'materialSupplier', vendor_id
                                   ),
                                 'details', jsonb_build_object(),
                                 'isPackage', false,
                                 'orderFormat', 'Physical Resource',
                                 'vendorDetail', jsonb_build_object('vendorAccount', 'libraryorders@library.tam'),
                                 'titleOrPackage', 'ABA Journal',
                                 'automaticExport', true,
                                 'publicationDate', '1915-1983',
                                 'purchaseOrderId', order_id,
                                 'poLineNumber', concat(ponumber, '-', line_counter),
                                 'claims', json_build_array(),
                                 'metadata', jsonb_build_object(
                                   'createdDate', '2023-02-08T00:00:00.000+0000',
                                   'createdByUserId', '28d1057c-d137-11e8-a8d5-f2801f1b9fd1',
                                   'updatedDate', '2023-02-08T00:00:00.000+0000',
                                   'updatedByUserId', '28d1057c-d137-11e8-a8d5-f2801f1b9fd1'
                                   )
                ));
    end loop;
END
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION public.run_acq_samples_generator() RETURNS VOID as
$$
DECLARE
  -- !!! SET DEFAULT TENANT NAME !!!
  tenant             text DEFAULT 'diku';
  org_to_generate    integer DEFAULT 1; -- amount of organizations to be created. keep 1 if vendor exists
  orders_to_generate integer DEFAULT 1; -- amount of orders per organization
  lines_to_generate  integer DEFAULT 1; -- amount of PO lines per order

BEGIN
  -- CREATE sample data
  PERFORM public.generate_vendors(org_to_generate, orders_to_generate, lines_to_generate, tenant);
END
$$ LANGUAGE plpgsql;


-- RUN SCRIPT
-- before running the script set all DEFAULT values in DECLARE sections according to you tenant data
select public.run_acq_samples_generator();

-- CLEANUP
DROP FUNCTION IF EXISTS public.generate_polines(text, integer, integer, text);
DROP FUNCTION IF EXISTS public.generate_orders(integer, integer, text, text);
DROP FUNCTION IF EXISTS public.generate_vendors(integer, integer, integer, text);
DROP FUNCTION IF EXISTS public.run_acq_samples_generator();


