var pasteboardExpirationInMinutes = 10 * 60;

function jmiGetSelectedIds() {
  var formElement = $('objectItems');
  if (formElement == undefined)
    return [];
  
  var ids = Form.getInputs(formElement, 'checkbox', 'ids:list');
  var selIds = []
  for (var i = 0; i < ids.length; i++) {
    if (ids[i].checked)
      selIds[selIds.length] = ids[i].value;
  }
  return selIds;
}
function jmiSelectAllIds() {
  var ids = Form.getInputs($('objectItems'), 'checkbox', 'ids:list');
  for (var i = 0; i < ids.length; i++)
    ids[i].checked = true;
}
function jmiDeselectAllIds() {
  var ids = Form.getInputs($('objectItems'), 'checkbox', 'ids:list');
  for (var i = 0; i < ids.length; i++)
    ids[i].checked = false;
}

function jmiCutCopyObjects(sender, operation) {
  if (navigator.cookieEnabled == false) {
    alert("JMI cannot use pasteboard when cookies are disabled.");
    return;
  }
  
  var expires = new Date();
  expires.setTime(expires.getTime() +
                  pasteboardExpirationInMinutes * 60 * 1000);
  var cookieString = "jmi.pasteboard=" + operation + "|";
  cookieString += clientObject.baseURL;
  
  var ids = jmiGetSelectedIds();
  for (var i = 0; i < ids.length; i++)
    cookieString += "|" + ids[i];
  
  cookieString += "; path=/; expires=" + expires.toGMTString();
  
  document.cookie = cookieString;
  
  jmiUpdatePasteboard();
}

function jmiUpdatePasteboard() {
  new Ajax.Updater('jmiPasteboard',
    clientObject.baseURL + "/-manage_workspace",
    {
      method:       'get',
      asynchronous: false,
      parameters:   'wofid=jmiPasteboard'
    });
}

function jmiToggleSelection(sender) {
  if (sender.checked)
    jmiSelectAllIds();
  else
    jmiDeselectAllIds();
}
