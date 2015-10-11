SlackElasticsearchImport
===

## Features
- [x] Slackのチャネル一覧取得
- [x] Slackのコメント取得
- [x] Akkaでの並行処理
- [ ] 並行処理数の制限
- [ ] コマンドライン引数の値をオプションに反映
- [ ] Elasticsearchへのインポート処理
- [ ] 処理完了を検知して各Actorを停止
- [ ] 各種エラー処理

## 実行
実行： `$ ./activator 'runMain com.hideto0710.main.SlackElasticsearchImport --from 100'`  
ユニットテスト： `$ ./activator test`  
個別ユニットテスト： `$ ./activator 'testOnly com.hideto0710.slack.SlackApiClientSpec'`
ドキュメント： `$ ./activator doc`

## 概要
`Slack API` で取得したデータを `Elasticsearch` にインポートする `Scala` プログラム。（並行処理に `Akka` を利用）

- [Scala](http://www.scala-lang.org/)
- [Elasticsearch](https://www.elastic.co/products/elasticsearch)
- [Akka](http://akka.io/)
- [Slack API](https://api.slack.com/)