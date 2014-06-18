define [
  'apps/Tree/models/DocumentList'
], (DocumentList) ->
  describe 'apps/Tree/models/DocumentList', ->
    class DocumentSet extends Backbone.Model
      initialize: ->
        @searchResults = new Backbone.Collection

    class Tag extends Backbone.Model

    describe 'normally', ->
      beforeEach ->
        @documentSet = new DocumentSet()
        @params =
          documentSet: @documentSet
          toApiParams: -> { tags: '2' }
          equals: -> true
          reset:
            byDocument: (document) ->
              documentSet: @documentSet
              document: document
              equals: -> false

        @list = new DocumentList({}, {
          params: @params
          url: '/documentsets/1/documents'
        })
        @docs = @list.documents

      afterEach ->
        @documentSet.off()
        @list.stopListening()
        @list.off()
        @docs.off()

      it 'should set params', -> expect(@list.params).to.eq(@params)
      it 'should be empty', -> expect(@docs.pluck('type')).to.deep.eq([])
      it 'should start with length=null', -> expect(@list.get('length')).to.be.null
      it 'should have nDocumentsPerPage=20', -> expect(@list.nDocumentsPerPage).to.eq(20)
      it 'should have nPagesFetched=0', -> expect(@list.get('nPagesFetched')).to.eq(0)

      describe 'on first .fetchNextPage()', ->
        beforeEach ->
          @sandbox = sinon.sandbox.create(useFakeServer: true, useFakeTimers: true)
          @list.fetchNextPage()
          @sandbox.clock.tick(1)

        afterEach -> @sandbox.restore()

        it 'should have a loading placeholder', -> expect(@docs.pluck('type')).to.deep.eq(['loading'])
        it 'should have length=null', -> expect(@list.get('length')).to.be.null
        it 'should have nPagesFetched=0', -> expect(@list.get('nPagesFetched')).to.eq(0)
        it 'should have isComplete=false', -> expect(@list.isComplete()).to.be.false
        it 'should request /documents', ->
          expect(@sandbox.server.requests.length).to.eq(1)
          req = @sandbox.server.requests[0]
          expect(req.method).to.eq('GET')
          expect(req.url).to.eq('/documentsets/1/documents?tags=2&pageSize=20&page=1')
        it 'should return the same promise and not change anything when calling again', ->
          p1 = @list.fetchNextPage()
          p2 = @list.fetchNextPage()
          @sandbox.clock.tick(1)
          expect(p1).to.eq(p2)
          expect(@docs.pluck('type')).to.deep.eq([ 'loading' ])

        describe 'on error', ->
          beforeEach ->
            @sandbox.stub(console, 'log')
            @sandbox.server.requests[0].respond(404, {}, '')
            @sandbox.clock.tick(1)

          it 'should log the error', -> expect(console.log).to.have.been.called
          it 'should leave the loading document there', -> expect(@docs.pluck('type')).to.deep.eq([ 'loading' ])
          it 'should not retry too quickly', ->
            @sandbox.clock.tick(200)
            expect(@sandbox.server.requests.length).to.eq(1)
          it 'should retry eventually', ->
            @sandbox.clock.tick(5000)
            expect(@sandbox.server.requests.length).to.eq(2)
          it 'should not add a second loading document on retry', ->
            @sandbox.clock.tick(5000)
            expect(@docs.pluck('type')).to.deep.eq([ 'loading' ])

        describe 'on zero-doc success', ->
          beforeEach ->
            @sandbox.server.requests[0].respond(200, { 'Content-Type': 'application/json' }, JSON.stringify(
              documents: []
              total_items: 0
            ))
            @sandbox.clock.tick(1)

          it 'should remove the loading document', -> expect(@docs.length).to.eq(0)
          it 'should set length', -> expect(@list.get('length')).to.eq(0)
          it 'should have nPagesFetched=1', -> expect(@list.get('nPagesFetched')).to.eq(1)
          it 'should have isComplete=true', -> expect(@list.isComplete()).to.be.true
          it 'should return a resolved promise on fetchNextPage()', (done) ->
            @list.fetchNextPage().then((x) -> expect(x).to.be.null; done())
            @sandbox.clock.tick(1)

        describe 'on a-few-docs success', ->
          beforeEach ->
            @sandbox.server.requests[0].respond(200, { 'Content-Type': 'application/json' }, JSON.stringify(
              documents: [ { id: 1 }, { id: 2 }, { id: 3 } ]
              total_items: 3
            ))
            @sandbox.clock.tick(1)

          it 'should populate with the documents', -> expect(@docs.pluck('id')).to.deep.eq([ 1, 2, 3 ])
          it 'should set length', -> expect(@list.get('length')).to.eq(3)
          it 'should have nPagesFetched=1', -> expect(@list.get('nPagesFetched')).to.eq(1)
          it 'should have isComplete=true', -> expect(@list.isComplete()).to.be.true

        describe 'on one-page success', ->
          beforeEach ->
            @sandbox.server.requests[0].respond(200, { 'Content-Type': 'application/json' }, JSON.stringify(
              documents: ({ id: x } for x in [ 1 .. @list.nDocumentsPerPage ])
              total_items: @list.nDocumentsPerPage + 1
            ))
            @sandbox.clock.tick(1)

          it 'should populate with the documents', -> expect(@docs.length).to.eq(@list.nDocumentsPerPage)
          it 'should set length', -> expect(@list.get('length')).to.eq(@list.nDocumentsPerPage + 1)
          it 'should have isComplete=false', -> expect(@list.isComplete()).to.be.false

          it 'should tag the list, client-side', ->
            tag = new Tag(name: 'a tag')
            @documentSet.trigger('tag', tag, @params)
            expect(@docs.at(0).hasTag(tag)).to.be.true
            expect(@docs.at(1).hasTag(tag)).to.be.true

          it 'should untag the list, client-side', ->
            tag = new Tag(name: 'a tag')
            @docs.at(0).tag(tag)
            @documentSet.trigger('untag', tag, @params)
            expect(@docs.at(0).hasTag(tag)).to.be.false
            expect(@docs.at(1).hasTag(tag)).to.be.false

          it 'should untag a document, client-side', ->
            tag = new Tag(name: 'a tag')
            @documentSet.trigger('tag', tag, @params.reset.byDocument(@docs.at(0)))
            expect(@docs.at(0).hasTag(tag)).to.be.true
            expect(@docs.at(1).hasTag(tag)).to.be.false

          it 'should trigger nothing when tagging or untagging a single document', ->
            tag = new Tag(name: 'a tag')
            @list.on('all', spy = sinon.spy())
            @documentSet.trigger('tag', tag, @params.reset.byDocument(@docs.at(0)))
            @documentSet.trigger('untag', tag, @params.reset.byDocument(@docs.at(0)))
            expect(spy).not.to.have.been.called

          describe 'on subsequent fetchNextPage()', ->
            beforeEach ->
              @list.fetchNextPage()
              @sandbox.clock.tick(1)

            it 'should have nPagesFetched=1', -> expect(@list.get('nPagesFetched')).to.eq(1)
            it 'should send a new request', ->
              expect(@sandbox.server.requests.length).to.eq(2)
              req = @sandbox.server.requests[1]
              expect(req.method).to.eq('GET')
              expect(req.url).to.eq('/documentsets/1/documents?tags=2&pageSize=20&page=2')
            it 'should add a loading document', -> expect(@docs.last().get('type')).to.eq('loading')

            describe 'on success', ->
              beforeEach ->
                @sandbox.server.requests[1].respond(200, { 'Content-Type': 'application/json' }, JSON.stringify(
                  documents: [ { id: @list.nDocumentsPerPage + 1 } ]
                  total_items: @list.nDocumentsPerPage + 1
                ))
                @sandbox.clock.tick(1)

              it 'should have nPagesFetched=2', -> expect(@list.get('nPagesFetched')).to.eq(2)
              it 'should have isComplete=true', -> expect(@list.isComplete()).to.be.true
              it 'should have all documents', -> expect(@docs.pluck('id')).to.deep.eq([ 1 .. (@list.nDocumentsPerPage + 1) ])
              it 'should return a resolved promise on fetchNextPage()', ->
                @list.fetchNextPage().then((x) -> expect(x).to.be.null; done())
                @sandbox.clock.tick(1)

    describe 'with an unfinished SearchResult', ->
      class SearchResult extends Backbone.Model
        idAttribute: 'query'

      beforeEach ->
        @documentSet = new DocumentSet()
        @params =
          documentSet: @documentSet
          searchResult: new SearchResult(query: 'foo')
          toApiParams: -> { searchResults: '2' }

        @list = new DocumentList({}, {
          params: @params
          url: '/documentsets/1/documents'
        })
        @docs = @list.documents

      afterEach ->
        @documentSet.off()
        @list.stopListening()
        @list.off()
        @docs.off()

      it 'should be isComplete() to begin with', -> expect(@list.isComplete()).to.be.true

      describe 'when the searchResult changes', ->
        beforeEach -> @params.searchResult.set(state: 'Complete')

        it 'should have isComplete() false', -> expect(@list.isComplete()).to.be.false
        it 'should have no length', -> expect(@list.get('length')).to.be.null
        it 'should have no pages fetched', -> expect(@list.get('nPagesFetched')).to.eq(0)
