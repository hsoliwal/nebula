export default function Orders() {
  return (<M3VerticalPanel>
    <M3Label id="title" text="Orders &amp; shipping" />
    <M3Label id="description" text="A browser & native viewport candidate" />
    <M3Button id="refresh" text="Refresh orders" />
    <M3Button id="cancel" text="Cancel" disabled />
  </M3VerticalPanel>);
}
