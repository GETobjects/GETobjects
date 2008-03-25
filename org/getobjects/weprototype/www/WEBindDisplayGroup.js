function WESortOrdering(_key, _dir) {
  this.key      = _key;
  this.selector = _dir;
  
  this.queryValue = function() {
    if (this.selector) {
      if ("D" == this.selector)
        return "-" + escape(this.key);
      if ("A" != this.selector)
        return escape(this.key) + "-" + this.selector;
    }
    return escape(this.key);
  };
  
  this.nextSortDirection = function() {
    if (this.selector) {
      if (this.selector == "A")  return "D";
      if (this.selector == "D")  return "A";
      if (this.selector == "AI") return "DI";
      if (this.selector == "DI") return "AI";
    }
    return "D";
  };
}

function WEBindDisplayGroup() {
  this.updateCounter     = 0;
  this.currentRequest    = null;
  
  /* index */
  
  this.indexOfFirstDisplayedObject = function() {
    if (this.currentBatchIndex < 1) {
      alert("invalid batch index: " + this.currentBatchIndex);
      return 0;
    }
    
    if (this.numberOfObjectsPerBatch < 1)
      return 0;
    
    return (this.currentBatchIndex - 1) * this.numberOfObjectsPerBatch;
  };
  
  this.indexOfLastDisplayedObject = function() {
    if (this.numberOfObjectsPerBatch == 0)
      return this.count - 1;
    
    var fdo = this.indexOfFirstDisplayedObject();
    
    if ((fdo + this.numberOfObjectsPerBatch) < this.count)
      return (fdo + this.numberOfObjectsPerBatch - 1);
    
    return (this.count - 1);
  };
  
  /* batches */
  
  this.isFirstBatch = function() {
    return this.currentBatchIndex < 2;
  };
  this.isLastBatch = function() {
    return this.currentBatchIndex >= this.batchCount;
  };
  this.isInnerBatch = function() {
    return this.currentBatchIndex > 1 && !this.isLastBatch();
  };

  this.nextBatchIndex = function() {
    return (this.isLastBatch() ? 1 : this.currentBatchIndex + 1);
  };
  this.previousBatchIndex = function() {
    return (this.isFirstBatch() ? this.batchCount : this.currentBatchIndex-1);
  };
  
  /* URLs */
  
  this.queryDictionary = function(_fragment) {
    var qp = window.location.search;
    
    if (qp == undefined)
      qp = $H({});
    else {
      if (qp.length > 0)
        qp = $H(qp.toQueryParams());
      else
        qp = $H({});
    }
    
    /* batch index and count */
    
    if (this.currentBatchIndex > 0)
      qp[this.qpPrefix + this.qpIndex] = this.currentBatchIndex;
    
    if (this.numberOfObjectsPerBatch > 0)
      qp[this.qpPrefix + this.qpBatchSize] = this.numberOfObjectsPerBatch;
    
    /* orderings */
    
    if (this.sortOrderings) {
      var sos = "";
      for (var i = 0; i < this.sortOrderings.length; i++) {
        if (i != 0) sos = sos + ",";
        sos += this.sortOrderings[i].queryValue();
      }
      qp[this.qpPrefix + this.qpOrderKey] = sos;
    }
    
    /* queryMatch, queryOperator, queryMin, queryMax */
    
    if (this.queryMatch) {
      for (var key in this.queryMatch)
        qp[this.qpPrefix + this.qpMatchPrefix + key] = this.queryMatch[key];
    }
    if (this.queryOperator) {
      for (var key in this.queryOperator)
        qp[this.qpPrefix + this.qpOpPrefix + key] = this.queryOperator[key];
    }
    if (this.queryMin) {
      for (var key in this.queryMin)
        qp[this.qpPrefix + this.qpMinPrefix + key] = this.queryMin[key];
    }
    if (this.queryMax) {
      for (var key in this.queryMax)
        qp[this.qpPrefix + this.qpMaxPrefix + key] = this.queryMax[key];
    }
    
    // TODO
    
    /* fragment */
    
    if (_fragment) {
      qp['wofid'] = _fragment;
    }
    
    return qp;
  };
  
  this.weOnSuccess = function(_request) {
    //alert("did it: " + this.currentBatchIndex);
    if (this.fragment)
      Element.update(this.fragment, _request.responseText);
    
    if (this.onChange)
      this.onChange(this);
  };
  
  this.weOnComplete = function(_request) {
    this.currentRequest = null;

    if (this.onFinishFetch)
      this.onFinishFetch(this);
  };
  
  /* async IO */
  
  this.abort = function() {
    if (this.currentRequest != null) {
      try {
        this.currentRequest.transport.abort();
      }
      catch (e) {}
      this.currentRequest = null;
    }
  };
  
  this.runRequest = function(qp) {
    this.abort();
    
    if (this.onStartFetch) {
      if (!this.onStartFetch(this))
        return false;
    }
    
    this.updateCounter++;
    //qp['__up__'] = this.updateCounter;
    
    var qs = qp.toQueryString();
    // alert("Q: " + qs);
    
    var url = window.location.pathname;

    /* context */
    this.currentRequest = new Ajax.Request(url,
      { method:       'get', 
        asynchronous: true,
        evalScripts:  true,
        parameters:   qs,
        onSuccess:    this.weOnSuccess.bind(this),
        onComplete:   this.weOnComplete.bind(this),
        onFailure:    function(t) {
         alert("Error updating display group!");
        }
      }
    );
    
    return true;
  };
  
  /* updating */
  
  this.update = function(fragment, action) {
    if (action == undefined)
      ;
    else if (action == "next")
      this.currentBatchIndex = this.nextBatchIndex();
    else if (action == "previous")    
      this.currentBatchIndex = this.previousBatchIndex();
    else
      alert("Unknown update action: " + action);
    
    this.fragment = fragment;
    
    this.runRequest(this.queryDictionary(fragment));
    
    return false; /* for use in event handlers, cancel exec */
  };
  
  /* sorting */
  
  this.doSort = function(fragment, sortField) {
    this.currentBatchIndex = 1;
    
    if (this.sortOrderings) {
      var so = this.sortOrderings[0];
      if (so.key == sortField)
        so.selector = so.nextSortDirection();
      else
        this.sortOrderings = null;
    }
    
    if (this.sortOrderings == null)
      this.sortOrderings = [ new WESortOrdering(sortField, "A") ];

    this.fragment = fragment;
    
    this.runRequest(this.queryDictionary(fragment));
    
    return false; /* for use in event handlers, cancel exec */
  };
}
