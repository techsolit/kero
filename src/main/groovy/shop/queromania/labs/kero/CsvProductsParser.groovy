package shop.queromania.labs.kero

import groovy.json.JsonBuilder
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord

class CsvProductsParser {

    final Map<String, Integer> indexes = [
            uniqueUrl: 0, name: 1, categories: 2, size: 4, color: 6, price: 9, discountPrice: 10, sku: 16,
            display  : 18, description: 20, tags: 21, seoTitle: 22, seoDescription: 23
    ]

    static main(args) {
        /** TODO refactor to accept -i and -o */
        new File('outputs/products-exported.json').write(new JsonBuilder(
                new CsvProductsParser().parse('inputs/exported/produtos-export-nuvemshop-20180826.csv')
        ).toPrettyString(), 'UTF-8')
    }

    Map parse(String input) {
        def products = [:] as Map

        CSVParser.parse(
                new FileReader(input),
                CSVFormat.DEFAULT
                        .withFirstRecordAsHeader()
        ).each { CSVRecord line ->
            def uniqueUrl = line.get(indexes.uniqueUrl)
            def discountPriceStr = line.get(indexes.discountPrice).trim()

            def product = (uniqueUrl in products.keySet()) ?
                    addVariations(
                            products[uniqueUrl] as Map,
                            line.get(indexes.color),
                            line.get(indexes.size)
                    ) :
                    [
                            uniqueUrl      : uniqueUrl.trim(),
                            name           : line.get(indexes.name)?.trim(),
                            categories     : line.get(indexes.categories)?.split(/,/)
                                    ?.collect { it.trim() } ?: [],
                            sizes          : [line.get(indexes.size)?.trim()],
                            colors         : [line.get(indexes.color)?.trim()],
                            price          : Utils.asNumber(line.get(indexes.price)),
                            sku            : line.get(indexes.sku)?.padLeft(6, '0'),
                            display        : line.get(indexes.display)?.trim()?.toUpperCase() == 'SIM',
                            descriptionHtml: line.get(indexes.description)
                                    ?.replaceAll(/[\r\t\n\s]+/, ' '),
                            seo            : [
                                    title      : line.get(indexes.seoTitle),
                                    description: line.get(indexes.seoDescription)
                            ]
                    ]
            if (discountPriceStr) {
                product << [discountPrice: Utils.asNumber(discountPriceStr)]
            }
            products << [(uniqueUrl): product]
        }

        return products
    }

    static private Map addVariations(Map product, String color, String size) {
        def colors = (product.colors as List) ?: []
        if (!(color in colors)) {
            product << [colors: colors + [color]]
        }

        def sizes = (product.sizes as List) ?: []
        if (!(size in sizes)) {
            product << [sizes: sizes + [size]]
        }

        return product
    }
}
